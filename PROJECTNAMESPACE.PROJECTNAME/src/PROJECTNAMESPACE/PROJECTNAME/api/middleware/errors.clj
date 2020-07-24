(ns PROJECTNAMESPACE.PROJECTNAME.api.middleware.errors
  (:require [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [clojure.tools.reader.edn :as edn]
            [hiccup.page :as hiccup]
            [jsonista.core :as jsonista]
            [expound.alpha :as expound]
            [reitit.coercion :as coercion]
            [reitit.ring :as ring]
            [reitit.ring.middleware.exception :as exception]
            [ring.util.http-response :as response]
            [PROJECTNAMESPACE.PROJECTNAME.api.errors :as errors]))

(defn browser-request?
  [request]
  (= "text/html" (-> request
                     :muuntaja/response
                     :raw-format)))

(defn title-case
  "Turns something like `:foo_bar-foo-bar` into \"Foo: Bar Foo Bar"
  [s]
  (-> s
      name
      (str/replace "_" ":-")
      (str/split #"-")
      (#(map str/capitalize %))
      (#(str/join " " %))))

(defn coercion-error-handler [status]
  (let [printer (expound/custom-printer {:theme :figwheel-theme, :print-specs? false})
        handler (exception/create-coercion-handler status)]
    (fn [exception request]
      (printer (-> exception ex-data :problems))
      (handler exception request))))

(defn api-response
  ([error]
   (api-response error false))
  ([{:keys [type code message info status]} as-json?]
   (cond-> {:status status
            :body {:error_type (csk/->snake_case type)
                   :error_code (csk/->snake_case code)
                   :error_message message
                   :info (cske/transform-keys csk/->snake_case info)
                   :http_status status}}
     as-json? (response/content-type "application/json")
     as-json? (update :body jsonista/write-value-as-string))))

(defn browser-response
  [{:keys [type code message info status]}]
  (-> {:status status
       :body (hiccup/html5
              {:lang "en"}
              [:head
               [:meta {:charset "utf-8"}]
               [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
               [:meta
                {:name "viewport"
                 :content "width=device-width, initial-scale=1.0, maximum-scale=1.0"}]
               [:link {:rel "icon" :href "/favicon.ico" :type "image/x-icon"}]
               [:body
                [:div
                 {:style "font-family: sans-serif; text-align: center"}
                 [:h1.error-title (title-case type)]
                 [:h2.error-subtitle (title-case code)]
                 [:p.error-message message]
                 [:p.error-info
                  {:style "font-family: monospace; background: #ddd; padding: 1em"}
                  (pr-str (cske/transform-keys csk/->snake_case_string info))]]]])}
      (response/content-type "text/html")))

(defn render-response
  "Renders error as html or api response based on request type"
  ([request error]
   (render-response request error false))
  ([request error as-json?]
   (if (browser-request? request)
     (browser-response error)
     (api-response error as-json?))))

(defn default-handler
  "Default handler for exceptions that don't match any other handler"
  [registry]
  (fn [e request]
    (log/error e "Uncaught exception was thrown")
    (->> (errors/exception :internal-server-error)
         (errors/error-context registry)
         (render-response request))))

(defn http-response-handler
  "Used to override error handling and return specific response"
  [e _request]
  (-> e ex-data :response))

(defn request-parsing-handler
  [registry]
  (fn [e request]
    (->> (errors/exception :invalid-body {:content-type (-> e ex-data :format pr-str)})
         (errors/error-context registry)
         (render-response request))))

(defn third [coll] (when (coll? coll) (nth coll 2)))

(defn parse-missing-fields
  [coercion-problems]
  (try (some->> coercion-problems
                (map (comp third third edn/read-string :pred))
                (remove nil?)
                (map csk/->snake_case_string))
       (catch Exception _ nil)))

(defn coercion-request-handler
  [registry]
  (fn [e request]
    (log/info "Error occurred coercing request"
              {:problems (:problems (coercion/encode-error (ex-data e)))}
              :uri (:uri request)
              :params (:params request))
    (let [problems (:problems (coercion/encode-error (ex-data e)))
          {:keys [in val]} (first problems)
          exception (if-let [key (last in)]
                      (errors/exception :invalid-field {:key (name key)
                                                        :value (pr-str val)})
                      (if-let [info (parse-missing-fields problems)]
                        (errors/exception :missing-field info)
                        (errors/exception :missing-field)))]
      (->> exception
           (errors/error-context registry)
           (render-response request)))))

(defn coercion-response-handler
  [registry]
  (fn [e request]
    (log/error e "Error occurred coercing response")
    (->> (errors/exception :internal-server-error)
         (errors/error-context registry)
         (render-response request))))

(defn api-exception-handler
  [registry]
  (fn [e request]
    (->> e
         (errors/error-context registry)
         (render-response request))))

(defn default-handlers
  [registry]
  {::exception/default (default-handler registry)
   ::ring/response http-response-handler
   :muuntaja/decode (request-parsing-handler registry)
   ::coercion/request-coercion (coercion-request-handler registry)
   ::coercion/reponse-coercion (coercion-response-handler registry)
   ::errors/exception (api-exception-handler registry)})

(def PROJECTNAMESPACE.PROJECTNAME-errors
  {:name ::PROJECTNAMESPACE.PROJECTNAME-errors
   :wrap (fn [handler error-registry & [overrides]]
           (let [mw (exception/create-exception-middleware
                     (merge
                      exception/default-handlers
                      (default-handlers error-registry)
                      {:reitit.coercion/request-coercion (coercion-error-handler 400)
                       :reitit.coercion/response-coercion (coercion-error-handler 500)}))]
             ((:wrap mw) handler)))})

(defn not-acceptable [registry]
  (fn [request]
    (let [error (->> (errors/exception :not-acceptable {:uri (:uri request)})
                     (errors/error-context registry))]
      (render-response request error true))))

(defn method-not-allowed [registry]
  (fn [request]
    (let [error (->> (errors/exception :method-not-allowed {:uri (:uri request)
                                                            :method (:request-method request)})
                     (errors/error-context registry))]
      (render-response request error true))))

(defn not-found [registry]
  (fn [request]
    (let [error (->> (errors/exception :not-found {:uri (:uri request)})
                     (errors/error-context registry))]
      (render-response request error true))))

(defn default-error-handler [registry]
  {:not-found (not-found registry)
   :method-not-allowed (method-not-allowed registry)
   :not-acceptable (not-acceptable registry)})

