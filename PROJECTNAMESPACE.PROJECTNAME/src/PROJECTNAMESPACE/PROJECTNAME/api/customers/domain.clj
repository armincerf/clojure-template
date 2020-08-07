(ns PROJECTNAMESPACE.PROJECTNAME.api.customers.domain
  "Customer routes implementation. A customer is an abstraction that unifies a
  partner account and a consumer account. It captures a consumer's relationship
  to a partner. E.g. Once a consumer has subscribed to a partner's plan, they
  become a customer of that partner."
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok]]
            [spec-tools.core :as st]
            [spec-tools.data-spec :as ds]
            [crux.api :as crux]
            [PROJECTNAMESPACE.PROJECTNAME.api.customers.model :as customers.model]
            [PROJECTNAMESPACE.PROJECTNAME.common :as common]
            [PROJECTNAMESPACE.PROJECTNAME.api.ids :as ids]
            [PROJECTNAMESPACE.PROJECTNAME.api.spec :as spec]))

(defn customer-id [req]
  (when-let [id (get-in req [:path-params :id])]
    (keyword "customer" id)))

(def customer-attrs
  {:customer/email ::spec/email
   :id ids/customer?
   :customer/name string?
   :customer/company string?
   :customer/location string?
   :customer/phone string?})

(s/def ::customer-ext
  (ds/spec {:name ::customer-ext
            :spec customer-attrs}))

(s/def :customer/ext
  (st/spec {:swagger/example {:customer/email "example@email.com"
                              :customer/name "bob"
                              :customer/company "ACME"
                              :customer/location "123 City of Town"
                              :customer/phone "012345679"
                              :id "cust231"}
            :spec ::customer-ext}))

(defn external-view
  [customer]
  (st/select-spec :customer/ext (common/add-external-id customer)))

(defn all-customers-handler
  [{:keys [node]} _req]
  (def node node)
  {:data (->> (customers.model/find-all (crux/db node))
              (mapv external-view))})

(defn customer-by-id-handler
  [req]
  {:data (-> req :properties :customer :data external-view)})

(defn update-customer-handler
  [_components req]
  (def req req)
  (let [id (customer-id req)
        db (-> req :properties :db)]
    (customers.model/update-by-id! db id (:body-params req))
    (ok {:data {:id id :new (external-view (customers.model/find-by-id db id))}})))

(defn delete-customer-handler
  [_components req]
  (let [id (customer-id req)
        db (-> req :properties :db)]
    (customers.model/delete-by-id! db id)
    (ok {:data {:id id :active false}})))
