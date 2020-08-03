(ns PROJECTNAMESPACE.PROJECTNAME.api.dashboard.assets.routes
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok]]
            [PROJECTNAMESPACE.PROJECTNAME.api.dashboard.assets.model :as assets.model]
            [PROJECTNAMESPACE.PROJECTNAME.api.dashboard.assets.domain :as assets.domain]))

(defn routes
  [components]
  ["assets" {:swagger {:tags ["Assets"]}}
   [[""
     {:name ::assets-collection
      :get
      {:summary "Retrieve all assets"
       :responses {200 {:body {:data (s/coll-of :asset/ext)}}}
       :handler (fn [req] (ok (assets.domain/all-assets-handler components req)))}}]
    ["/:id"
     {:name ::asset-resource
      :properties (fn [req] (assets.model/find-by-id (:node components) (assets.domain/asset-id req)))
      :parameters {:path {:id string?}}
      :get
      {:summary "Retrieve an asset"
       :responses {200 {:body :asset/ext}}
       :handler (fn [req] (ok (assets.domain/asset-by-id-handler req)))}
      :put
      {:summary "Update an asset"
       :responses {200 {:body :asset/ext}}
       :parameters {:body {:asset :asset/ext}}
       :handler (fn [req] (ok (assets.domain/update-asset-handler components req)))}
      :delete
      {:summary "Delete an asset"
       :responses {200 {:body :asset/inactive}}
       :handler (fn [req] (assets.domain/delete-asset-handler components req))}}]]])
