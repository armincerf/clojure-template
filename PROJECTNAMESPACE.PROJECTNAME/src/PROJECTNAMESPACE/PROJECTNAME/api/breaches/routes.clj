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
       :responses {200 {:body {:data (s/coll-of :breach/ext)}}}
       :handler (fn [req] (ok (breaches.domain/all-breaches-handler components req)))}}]
    ["/:asset-id"
     {:name ::breach-resource
      :properties (fn [req] (breaches.domain/find-for-asset components req))
      :parameters {:path {:asset-id string?}}
      :get
      {:summary "Retrieve a breach for the given asset ID"
       :responses {200 {:body :breach/ext}}
       :handler (fn [req] (ok (breaches.domain/breach-by-id-handler req)))}
      :put
      {:summary "Update an breach"
       :responses {200 {:body :breach/ext}}
       :parameters {:body {:breach :breach/ext}}
       :handler (fn [req] (ok (breaches.domain/update-breach-handler components req)))}
      :delete
      {:summary "Delete an breach"
       :responses {200 {:body :breach/inactive}}
       :handler (fn [req] (ok (breaches.domain/delete-breach-handler components req)))}}]]])

