(ns PROJECTNAMESPACE.PROJECTNAME.api.assets.domain
  "Asset routes implementation. A asset is a single piece of data which the
  customer has entered so that they can be alerted if it is found in any data
  breaches. This data could be an email, phone number, credit card etc."
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok]]
            [spec-tools.core :as st]
            [spec-tools.data-spec :as ds]
            [crux.api :as crux]
            [clojure.tools.logging :as log]
            [medley.core :as medley]
            [PROJECTNAMESPACE.PROJECTNAME.api.utils :as utils]
            [PROJECTNAMESPACE.PROJECTNAME.api.errors :as errors]
            [PROJECTNAMESPACE.PROJECTNAME.api.assets.model :as assets.model]
            [PROJECTNAMESPACE.PROJECTNAME.api.breaches.model :as breaches.model]
            [PROJECTNAMESPACE.PROJECTNAME.api.customers.model :as customers.model]
            [PROJECTNAMESPACE.PROJECTNAME.common :as common]
            [PROJECTNAMESPACE.PROJECTNAME.api.ids :as ids]
            [PROJECTNAMESPACE.PROJECTNAME.api.spec :as spec]))

(defn asset-id [req]
  (when-let [id (get-in req [:path-params :id])]
    (keyword "asset" id)))

(def asset-attrs
  {:id ids/asset?
   :asset/name string?
   :asset/description (s/nilable string?)
   :asset/type ::spec/asset-type
   :asset/data string?
   :asset/customer ids/customer?})

(s/def ::asset-ext
  (ds/spec {:name ::asset-ext
            :spec asset-attrs}))

(s/def :asset/ext
  (st/spec {:swagger/example {:id :asset/a12321
                              :customer :customer/a123
                              :name "Work email"
                              :data "example@email.com"
                              :type :email}
            :spec ::asset-ext}))

(defn external-view
  [asset]
  (when asset
    (st/select-spec ::asset-ext (common/add-external-id asset))))

(defn all-assets-handler
  [{:keys [node]} _req]
  (def node node)
  {:data (->> (assets.model/find-all (crux/db node))
              (mapv external-view))})

(defn asset-by-id-handler
  [_components req]
  (def req req)
  (let [asset (-> req :properties :asset external-view)
        db (utils/req->db req)]
    (when-not (:id asset)
      (throw (errors/exception
              :PROJECTNAMESPACE/asset-not-found
              {:id (asset-id req)})))
    {:data (assoc asset
                  :asset/breaches
                  (breaches.model/find-by-id db (:id asset)))}))

(defn add-asset-handler
  [{:keys [node]} req]
  (def req req)
  (let [asset (some-> req
                      :body-params
                      :asset
                      (assoc :crux.db/id (ids/asset)))
        db (crux/db node)
        _assert-asset-exists
        (when-not asset
          (throw (errors/exception :missing-field)))
        customer-id (:asset/customer asset)
        ;;hardcode until auth in place
        customer-id (:crux.db/id (first (customers.model/find-all db)))
        customer (customers.model/find-by-id db customer-id)]
    (when-not customer
      (throw (errors/exception
              :PROJECTNAMESPACE/customer-not-found {:id customer-id})))
    {:data (external-view (assets.model/insert!
                           node
                           (assoc asset
                                  :asset/customer customer-id)))}))

(defn update-asset-handler
  [{:keys [node]} req]
  (def req req)
  (let [id (asset-id req)
        asset (-> req
                  :body-params
                  :asset
                  (assoc :crux.db/id id)
                  (dissoc :id))
        db (utils/req->db req)
        customer-id (:asset/customer asset)
        customer (customers.model/find-by-id db customer-id)]
    (when-not customer
      (throw (errors/exception
              :PROJECTNAMESPACE/customer-not-found {:id customer-id})))
    (assets.model/update-by-id! db id asset)
    {:data (external-view (assets.model/find-by-id db id))}))

(defn delete-asset-handler
  [{:keys [node]} req]
  (let [id (asset-id req)
        db (utils/req->db req)]
    (assets.model/delete-by-id! db id)
    {:data {:id id :active false}}))
