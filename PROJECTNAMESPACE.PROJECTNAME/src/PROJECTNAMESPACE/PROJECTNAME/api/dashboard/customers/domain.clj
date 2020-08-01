(ns PROJECTNAMESPACE.PROJECTNAME.api.dashboard.customers.domain
  "Customer routes implementation. A customer is an abstraction that unifies a
  partner account and a consumer account. It captures a consumer's relationship
  to a partner. E.g. Once a consumer has subscribed to a partner's plan, they
  become a customer of that partner."
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok]]
            [spec-tools.core :as st]
            [spec-tools.data-spec :as ds]
            [PROJECTNAMESPACE.PROJECTNAME.api.dashboard.customers.model :as customers.model]
            [PROJECTNAMESPACE.PROJECTNAME.common :as common]
            [PROJECTNAMESPACE.PROJECTNAME.api.ids :as ids]
            [PROJECTNAMESPACE.PROJECTNAME.api.spec :as spec]))

(defn customer-id [req]
  (when-let [id (get-in req [:path-params :id])]
    (keyword "customer" id)))

(def customer-attrs
  {:email ::spec/email
   :id ids/customer?
   :name string?
   :company string?
   :location string?
   :phone string?})

(s/def :customer/active
  (ds/spec {:name :customer/active
            :spec (merge customer-attrs {:active true?})}))

(s/def :customer/inactive
  (ds/spec {:name :customer/inactive
            :spec (merge customer-attrs {:active false?})}))

(s/def :customer/ext
  (st/spec {:swagger/example {:email "example@email.com"
                              :active true
                              :partner-user-id 23
                              :id "cust231"}
            :spec (s/or
                   :active :customer/active
                   :inactive :customer/inactive)}))

(defn external-view
  [customer]
  (if (:active customer)
    (st/select-spec :customer/active customer)
    (st/select-spec :customer/inactive customer)))

(defn all-customers-handler
  [{:keys [node]} _req]
  (def node node)
  {:data (->> (customers.model/find-all node)
              (mapv common/prep-map)
              (mapv external-view))})

(defn customer-by-id-handler
  [req]
  {:data (-> req :properties :data external-view)})

(defn update-customer-handler
  [{:keys [node]} req]
  (def req req)
  (let [id (customer-id req)]
    (customers.model/update-by-id node id (:body-params req))
    (ok {:data {:id id :active false :new (customers.model/find-by-id node id)}})))

(defn delete-customer-handler
  [{:keys [node]} req]
  (let [id (customer-id req)]
    (customers.model/delete-by-id node id)
    (ok {:data {:id id :active false}})))
