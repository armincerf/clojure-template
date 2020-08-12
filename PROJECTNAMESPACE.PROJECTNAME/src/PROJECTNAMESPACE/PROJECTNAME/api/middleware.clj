(ns PROJECTNAMESPACE.PROJECTNAME.api.middleware
  (:require [cheshire.core :as json]
            [clojure.core.async :as async]
            [clojure.java.io :as io]
            [expound.alpha :as expound]
            [clojure.spec.alpha :as s]
            [manifold.deferred :as d]
            [reitit.ring.middleware.exception :as exception]
            [ring.core.protocols :refer [StreamableResponseBody]]
            [ring.middleware.not-modified :as not-modified]
            [ring.middleware.cookies :as ring.mw.cookies]
            [ring.middleware.proxy-headers :as proxy-headers]
            [ring.middleware.ssl :as ssl]
            [ring.middleware.x-headers :as x-headers]
            [ring.util.response :as resp]
            [ring.util.time :as ring.util.time]
            [PROJECTNAMESPACE.PROJECTNAME.api.session :as session]
            [PROJECTNAMESPACE.PROJECTNAME.api.auth.domain :as auth.domain]
            [PROJECTNAMESPACE.PROJECTNAME.api.errors :as errors]
            [PROJECTNAMESPACE.PROJECTNAME.api.utils :as utils])
  (:import java.time.Instant
           java.util.Date))

(defn wrap-no-cache
  "Add 'Cache-Control: no-cache' to every response.

   Used to avoid serving stale resources during development
   see: https://danielcompton.net/2018/03/21/how-to-serve-clojurescript"
  [handler]
  (fn [req respond raise]
    (handler req
             #(respond (update % :headers assoc "Cache-Control" "no-cache"))
             raise)))

(defn wrap-not-found
  [handler]
  (fn [req respond raise]
    (handler req
             #(respond (or % {:status 404}))
             raise)))

(defn remove-last-modified
  "Removes the Last-Modified header from a response

  ClojureScript uses the JS file modification times to detect changes to source files.
  This is not useful to provide when serving compiled ClojureScript, and can lead
  to both over and under-caching.
  See https://danielcompton.net/2018/03/21/how-to-serve-clojurescript for more details."
  [handler]
  (fn [req respond raise]
    (handler req
             (fn [resp] (respond (utils/remove-header resp "Last-Modified")))
             raise)))

(def not-modified
  {:name :ring.middleware.not-modified/wrap-not-modified
   :wrap not-modified/wrap-not-modified})

(defn parse-last-modified
  [properties]
  (when-let [last-modified (or (:last-modified properties)
                               (when-let [updated-at (:updated-at properties)]
                                 (Date/from ^Instant updated-at)))]
    (ring.util.time/format-date last-modified)))

(defn last-modified-response
  [request properties]
  (let [last-modified (parse-last-modified properties)
        ;; we build a fake response since ring middleware expects you are doing this
        ;; after processing the handler
        fake-resp {:status 200
                   :headers (cond-> {}
                              last-modified (assoc "Last-Modified" last-modified)
                              (:etag properties) (assoc "ETag" (:etag properties)))}
        resp (not-modified/not-modified-response fake-resp request)]
    ;; request/response suggest using a 304
    (when (= 304 (:status resp))
      resp)))

;; Allows for event-stream content-type
(extend-type clojure.core.async.impl.channels.ManyToManyChannel
  StreamableResponseBody
  (write-body-to-stream [channel response output-stream]
    (async/go (with-open [writer (io/writer output-stream)]
                (async/loop []
                  (when-let [msg (async/<! channel)]
                    (doto writer (.write msg) (.flush))
                    (recur)))))))

(defn stream-msg [payload]
  (str "data:" (json/encode payload) "\n\n"))

(def request-properties
  {:name ::request-properties
   :compile (fn properties-compile [{:keys [properties]} _]
              (when properties
                (fn properties-mw [handler]
                  (fn
                    ([request]
                     (let [properties (properties request)]
                       (if (false? (:exists? properties))
                         (throw (errors/exception :not-found {:uri (:uri request)}))
                         (if-let [resp (last-modified-response request properties)]
                           resp ;; appropriate to use 304 response
                           (handler (assoc request :properties properties))))))
                    ([request respond raise]
                     (let [properties (properties request)]
                       (if (false? (:exists? properties))
                         (throw (errors/exception :not-found {:uri (:uri request)}))
                         (if-let [resp (last-modified-response request properties)]
                           (respond resp)
                           (handler (assoc request :properties properties)
                                    respond
                                    raise)))))))))})

(defn wrap-ring-async-handler
  "Converts given asynchronous Ring handler to Aleph-compliant handler.
    More information about asynchronous Ring handlers and middleware:
  https://www.booleanknot.com/blog/2016/07/15/asynchronous-ring.html"
  [handler]
  (fn [request]
    (let [response (d/deferred)]
      (handler request #(d/success! response %) #(d/error! response %))
      response)))

(def deferred-handler
  {:name ::deferred-handler
   :wrap (fn [handler]
           (fn [request respond raise]
             (try
               (let [response (handler request)]
                 (if (d/deferred? response)
                   (d/on-realized response respond raise)
                   (respond response)))
               (catch Throwable t
                 (raise t)))))})

(def forwarded-proto
  {:name ::forwarded-proto
   :wrap ssl/wrap-forwarded-scheme})

(def forwarded-for
  {:name ::forwarded-for
   :wrap proxy-headers/wrap-forwarded-remote-addr})

(def session-middleware
  {:name ::session/wrap-session
   :wrap session/wrap-session})

(def cookies-middleware
  {:name ::ring.mw.cookies/wrap-cookies
   :wrap ring.mw.cookies/wrap-cookies})

(s/def ::authorize
  (s/or :handler :accessrules/handler :rule :accessrules/rule))

(def authorization-middleware
  {:name ::auth.domain/compile-authorization
   :spec (s/keys :req-un [::authorize])
   :compile auth.domain/compile-authorization})

(def authentication-middleware
  {:name ::auth.domain/wrap-authentication
   :wrap auth.domain/wrap-authentication})

(defn referrer-policy-response
  [response policy]
  (resp/header response "Referrer-Policy" (name policy)))

(defn wrap-referrer-policy
  "Sets a referrer policy for the response.
  See https://scotthelme.co.uk/a-new-security-header-referrer-policy/ for more details."
  [handler options]
  (fn
    ([request]
     (referrer-policy-response (handler request) options))
    ([request respond raise]
     (handler request #(respond (referrer-policy-response % options)) raise))))

(def security-headers
  {:name ::security-headers
   :description "Sets default security headers where we want a single option for all of our responses"
   :wrap (fn [handler]
           ((comp ssl/wrap-hsts
                  #(x-headers/wrap-content-type-options % :nosniff)
                  #(wrap-referrer-policy % :strict-origin-when-cross-origin))
            handler))})

(defn coercion-error-handler [status]
  (let [printer (expound/custom-printer {:theme :figwheel-theme, :print-specs? false})
        handler (exception/create-coercion-handler status)]
    (fn [exception request]
      (printer (-> exception ex-data :problems))
      (handler exception request))))

(def error-handling-middleware
  (exception/create-exception-middleware
   (merge
    exception/default-handlers
    {:reitit.coercion/request-coercion (coercion-error-handler 400)
     :reitit.coercion/response-coercion (coercion-error-handler 500)})))
