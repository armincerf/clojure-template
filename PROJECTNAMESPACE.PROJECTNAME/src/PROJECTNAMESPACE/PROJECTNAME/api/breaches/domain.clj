(ns PROJECTNAMESPACE.PROJECTNAME.api.breaches.domain
  "Breach routes implementation. A breach is a single piece of data which the
  customer has entered so that they can be alerted if it is found in any data
  breaches. This data could be an email, phone number, credit card etc."
  (:require [clojure.spec.alpha :as s]
            [cemerick.url :as url]
            [ring.util.http-response :refer [ok]]
            [aleph.http :as http]
            [buddy.core.keys :as k]
            [buddy.sign.jwt :as jwt]
            [byte-streams :as bs]
            [spec-tools.core :as st]
            [spec-tools.data-spec :as ds]
            [clojure.tools.logging :as log]
            [crux.api :as crux]
            [medley.core :as medley]
            [PROJECTNAMESPACE.PROJECTNAME.api.errors :as errors]
            [PROJECTNAMESPACE.PROJECTNAME.api.breaches.model :as breaches.model]
            [PROJECTNAMESPACE.PROJECTNAME.api.customers.model :as customers.model]
            [PROJECTNAMESPACE.PROJECTNAME.api.assets.model :as assets.model]
            [PROJECTNAMESPACE.PROJECTNAME.common :as common]
            [PROJECTNAMESPACE.PROJECTNAME.api.ids :as ids]
            [PROJECTNAMESPACE.PROJECTNAME.api.spec :as spec]
            [slingshot.slingshot :refer [throw+ try+]]
            [clojure.string :as str]))

(defn asset-id [req]
  (def req req)
  (when-let [id (get-in req [:path-params :asset-id])]
    (keyword "asset" id)))

(def breach-attrs
  {:breach/data map?
   :valid-time inst?})

(s/def ::breach-ext
  (ds/spec {:name ::breach-ext
            :spec breach-attrs}))

(defn external-view
  [breach]
  (st/select-spec ::breach-ext (common/add-external-id breach)))

(defn all-breaches-handler
  [{:keys [node]} _req]
  (def node node)
  {:data (->> (breaches.model/find-all (crux/db node))
              (mapv external-view))})

(defn breach-by-id-handler
  [req]
  (def req req)
  (let [breach (some-> req :properties external-view)]
    (when-not (:breach/data breach)
      (throw (errors/exception
              :PROJECTNAMESPACE/breach-not-found
              {:id (asset-id req)})))
    {:data breach}))

(defn delete-breach-handler
  [{:keys [node]} req]
  (let [id (asset-id req)]
    (breaches.model/delete-by-id! node id)
    {:data {:id id :active false}}))

(defn hibp-email-search
  [email]
  (def email email)
  (try+
    (:body @(http/get (str "https://haveibeenpwned.com/api/v3/breachedaccount/"
                           (url/url-encode email))
                      {:as :json
                       :headers {"hibp-api-key" "58779e4f5a4d427a9cb3175dcc3b3f58"
                                 "user-agent" "DataTest"}}))
    (catch [:status 404] {:keys [request-time headers body]}
      {})
    (catch Object _
      (throw+))))

(defn find-for-asset
  [{:keys [node]} req]
  (def req req)
  (def node node)
  (let [id (asset-id req)
        db (crux/db node)
        {:keys [asset/type asset/data] :as asset}
        (or (assets.model/find-by-id db id)
            (throw (errors/exception :PROJECTNAMESPACE/asset-not-found {:id id})))
        existing-breach (breaches.model/find-by-id db id)
        breach (merge
                existing-breach
                {:breach/data
                 (case type
                   :email (hibp-email-search data)
                   (throw (errors/exception
                           :PROJECTNAMESPACE/asset-type-invalid
                           {:asset asset :id id})))})]
    (prn "hi " breach existing-breach)
    (breaches.model/insert! node breach id)))

(defn breach-details-handler
  [req]
  (def req req)
  (let [breach-name (get-in req [:path-params :breach-name])]
    (def breach-name breach-name)
    (try+
     (:body @(http/get (str "https://haveibeenpwned.com/api/v3/breach/" breach-name)
                       {:as :json}))
     (catch [:status 404] {:keys [request-time headers body]}
       {})
     (catch Object _
       (throw+)))))
