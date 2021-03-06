(ns PROJECTNAMESPACE.PROJECTNAME.api.assets.routes
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok]]
            [crux.api :as crux]
            [PROJECTNAMESPACE.PROJECTNAME.api.spec :as spec]
            [PROJECTNAMESPACE.PROJECTNAME.api.assets.model :as assets.model]
            [PROJECTNAMESPACE.PROJECTNAME.api.assets.domain :as assets.domain]))

(defn routes
  [components]
  ["assets" {:swagger {:tags ["Assets"]}}
   [[""
     {:name ::assets-collection
      :post
      {:summary "Add a new asset"
       :responses {200 {:body {:data :asset/ext}}}
       :parameters {:body {:asset {:asset/data ::spec/non-blank-string
                                   :asset/name ::spec/non-blank-string
                                   :asset/type ::spec/asset-type}}}
       :handler (fn [req] (ok (assets.domain/add-asset-handler components req)))}
      :get
      {:summary "Retrieve all assets"
       :responses {200 {:body {:data (s/coll-of :asset/ext)}}}
       :handler (fn [req] (ok (assets.domain/all-assets-handler components req)))}}]
    ["/:id"
     {:name ::asset-resource
      :properties (fn [req]
                    (prn "foo")
                    (let [db (crux/db (:node components))
                          asset (assets.model/find-by-id
                                 db
                                 (assets.domain/asset-id req))]
                      {:db db
                       :asset asset}))
      :parameters {:path {:id string?}}
      :get
      {:summary "Retrieve an asset"
       :responses {200 {:body {:data :asset/ext}}}
       :handler (fn [req] (ok (assets.domain/asset-by-id-handler components req)))}
      :put
      {:summary "Update an asset"
       :responses {200 {:body {:data :asset/ext}}}
       :parameters {:body {:asset :asset/ext}}
       :handler (fn [req] (ok (assets.domain/update-asset-handler components req)))}
      :delete
      {:summary "Delete an asset"
       :handler (fn [req] (ok (assets.domain/delete-asset-handler components req)))}}]]])
