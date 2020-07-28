(ns PROJECTNAMESPACE.PROJECTNAME.api.dashboard.customers.routes
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok]]
            [PROJECTNAMESPACE.PROJECTNAME.api.dashboard.customers.domain :as customers.domain]))

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
      :properties (fn [req] (customers.domain/all-customers-handler components req))
      :parameters {:path {:id :customer/id}}
      :get
      {:summary "Retrieve a customer"
       :responses {200 {:body :customer/ext}}
       :handler (fn [req] (ok (customers.domain/customer-by-id-handler req)))}
      :delete
      {:summary "Delete a customer"
       :responses {200 {:body :customer/inactive}}
       :handler (fn [req] (customers.domain/delete-customer-handler components req))}}]]])

