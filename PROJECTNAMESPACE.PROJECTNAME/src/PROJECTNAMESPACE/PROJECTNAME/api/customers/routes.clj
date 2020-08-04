(ns PROJECTNAMESPACE.PROJECTNAME.api.customers.routes
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok]]
            [PROJECTNAMESPACE.PROJECTNAME.api.customers.model :as customers.model]
            [PROJECTNAMESPACE.PROJECTNAME.api.customers.domain :as customers.domain]))

(defn routes
  [components]
  ["customers" {:swagger {:tags ["Customers"]}}
   [[""
     {:name ::customers-collection
      :get
      {:summary "Retrieve all customers"
       :responses {200 {:body {:data (s/coll-of :customer/ext)}}}
       :handler (fn [req] (ok (customers.domain/all-customers-handler components req)))}}]
    ["/:id"
     {:name ::customer-resource
      :properties (fn [req] (customers.model/find-by-id (:node components)
                                                        (customers.domain/customer-id req)))
      :parameters {:path {:id string?}}
      :get
      {:summary "Retrieve a customer"
       :responses {200 {:body :customer/ext}}
       :handler (fn [req] (ok (customers.domain/customer-by-id-handler req)))}
      :put
      {:summary "Update a customer"
       :responses {200 {:body :customer/ext}}
       :parameters {:body :customer/ext}
       :handler (fn [req] (ok (customers.domain/update-customer-handler components req)))}
      :delete
      {:summary "Delete a customer"
       :responses {200 {:body :customer/inactive}}
       :handler (fn [req] (customers.domain/delete-customer-handler components req))}}]]])

