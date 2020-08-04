(ns PROJECTNAMESPACE.PROJECTNAME.api.router
  (:require [clojure.spec.alpha :as s]
            [integrant.core :as ig]
            [muuntaja.core :as m]
            [reitit.dev.pretty :as pretty]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            reitit.ring.spec
            [PROJECTNAMESPACE.PROJECTNAME.api.middleware :as mw]
            [PROJECTNAMESPACE.PROJECTNAME.api.middleware.errors :as mw.errors]
            [PROJECTNAMESPACE.PROJECTNAME.api.middleware.logging :as mw.logging]
            [spell-spec.alpha :as spell]))

(s/def ::routes (s/coll-of vector?))

(s/def ::middleware (s/coll-of some?))

(s/def ::request-diffs #{:trace :debug :info :none})

(s/def ::pretty-router-exceptions? boolean?)

(defmethod ig/pre-init-spec :PROJECTNAMESPACE.PROJECTNAME.api/router
  [_]
  (spell/keys :req-un [::routes]
              :opt-un [::middleware ::request-diffs
                       ::pretty-router-exceptions?]))

(defmethod ig/init-key :PROJECTNAMESPACE.PROJECTNAME.api/router
  [_ {:keys [routes middleware request-diffs]
      :or {middleware []}}]
  (ring/router
   [routes]
   (cond-> {:reitit.middleware/registry
            {;; query-params & form-params
             :parameters parameters/parameters-middleware
             ;; content-negotiation
             :format-negotiate muuntaja/format-negotiate-middleware
             ;; encoding response and error body
             :format-response muuntaja/format-response-middleware
             ;; custom error middleware
             :PROJECTNAMESPACE.PROJECTNAME-errors mw.errors/PROJECTNAMESPACE.PROJECTNAME-errors
             ;; decoding request body
             :format-request muuntaja/format-request-middleware
             ;; coercing response bodies
             :coerce-response coercion/coerce-response-middleware
             ;; coercing request parameters
             :coerce-request coercion/coerce-request-middleware
             ;; handling manifold
             :deferred mw/deferred-handler
             ;; authenticate jwt from cognito and add :identity property
             :identity mw/authentication-middleware
             ;; allow properties fns
             :properties mw/request-properties
             ;; allows not-modified responses
             :not-modified mw/not-modified}
            :validate reitit.ring.spec/validate
            :data {:muuntaja m/instance
                   :middleware middleware
                   :exception pretty/exception}}
     ;; pretty diffs - set to false if things are getting too slow
     false
     (assoc :reitit.middleware/transform
            (partial mw.logging/print-request-diffs request-diffs)))))
