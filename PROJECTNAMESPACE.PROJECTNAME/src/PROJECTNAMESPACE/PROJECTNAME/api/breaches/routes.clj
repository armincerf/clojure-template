(ns PROJECTNAMESPACE.PROJECTNAME.api.breaches.routes
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok]]
            [PROJECTNAMESPACE.PROJECTNAME.api.breaches.model :as breaches.model]
            [PROJECTNAMESPACE.PROJECTNAME.api.breaches.domain :as breaches.domain]))

(defn routes
  [components]
  ["breaches" {:swagger {:tags ["Breaches"]}}
   [[""
     {:name ::breaches-collection
      :get
      {:summary "Retrieve all breaches"

       :handler (fn [req] (ok (breaches.domain/all-breaches-handler components req)))}}]
    ["/:asset-id"
     {:name ::breach-resource
      :properties (fn [req] (breaches.domain/find-for-asset components req))
      :parameters {:path {:asset-id string?}}
      :get
      {:summary "Retrieve a breach for the given asset ID"

       :handler (fn [req] (ok (breaches.domain/breach-by-id-handler req)))}}]
    ["/:breach-name/details"
     {:name ::breach-details
      :parameters {:path {:breach-name string?}}
      :get
      {:summary "Retrieve breach details for the given name"

       :handler (fn [req] (ok {:data (breaches.domain/breach-details-handler req)}))}}]]])

