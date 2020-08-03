(ns PROJECTNAMESPACE.PROJECTNAME.api.dashboard.assets.domain
  "Asset routes implementation. A asset is a single piece of data which the
  customer has entered so that they can be alerted if it is found in any data
  breaches. This data could be an email, phone number, credit card etc."
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok]]
            [spec-tools.core :as st]
            [spec-tools.data-spec :as ds]
            [clojure.tools.logging :as log]
            [medley.core :as medley]
            [PROJECTNAMESPACE.PROJECTNAME.api.dashboard.assets.model :as assets.model]
            [PROJECTNAMESPACE.PROJECTNAME.common :as common]
            [PROJECTNAMESPACE.PROJECTNAME.api.ids :as ids]
            [PROJECTNAMESPACE.PROJECTNAME.api.spec :as spec]))

(defn asset-id [req]
  (when-let [id (get-in req [:path-params :id])]
    (keyword "asset" id)))

(def asset-attrs
  {:crux.db/id ids/asset?
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
  (st/select-spec ::asset (common/add-external-id asset)))

(defn all-assets-handler
  [{:keys [node]} _req]
  (def node node)
  {:data (->> (assets.model/find-all node)
              (mapv external-view))})

(defn asset-by-id-handler
  [req]
  (def req req)
  {:data (-> req :properties external-view)})

(defn update-asset-handler
  [{:keys [node]} req]
  (def req req)
  (let [id (asset-id req)]
    (assets.model/update-by-id node id ( (:asset (:params req))))
    (ok {:data {:id id :active false :new (assets.model/find-by-id node id)}})))

(defn delete-asset-handler
  [{:keys [node]} req]
  (let [id (asset-id req)]
    (assets.model/delete-by-id node id)
    (ok {:data {:id id :active false}})))
