(ns PROJECTNAMESPACE.PROJECTNAME.api.ring
  (:require [clojure.spec.alpha :as s]
            [co.deps.ring-etag-middleware :as etag]
            [integrant.core :as ig]
            reitit.core
            [reitit.ring :as ring]
            [spell-spec.alpha :as spell]
            [PROJECTNAMESPACE.PROJECTNAME.api.middleware :as mw]
            [PROJECTNAMESPACE.PROJECTNAME.api.middleware.errors :as mw.errors]))

(s/def ::resource-root string?)

(s/def ::router #(some->> % (satisfies? reitit.core/Router)))

(s/def ::status pos-int?)

(s/def ::error
  (spell/keys :req-un [::status]))

(s/def ::error-registry
  (s/map-of keyword? ::error))

(defmethod ig/pre-init-spec :PROJECTNAMESPACE.PROJECTNAME.api/ring
  [_]
  (spell/keys :req-un [::router ::error-registry ::resource-root]))

(defmethod ig/init-key :PROJECTNAMESPACE.PROJECTNAME.api/ring
  [_ {:keys [router error-registry resource-root]}]
  (let [dev? true] ;;TODO add environment
    (-> (ring/ring-handler
         router
         (ring/routes
          (cond-> (ring/create-resource-handler {:path "/"
                                                 :root "public"})
            true (mw/wrap-not-found)
            true (etag/wrap-file-etag)
            dev? (mw/remove-last-modified)
            dev? (mw/wrap-no-cache)
            true ((:wrap mw/not-modified)))
          (ring/create-default-handler (mw.errors/default-error-handler error-registry)))
         ;; This needs to wrap the entire handler to make it async all the way through, and to
         ;; avoid the retit ring-handler from getting confused.
         {:middleware [mw/wrap-ring-async-handler]}))))
