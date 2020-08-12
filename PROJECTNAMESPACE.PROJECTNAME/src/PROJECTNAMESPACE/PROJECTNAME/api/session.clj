(ns PROJECTNAMESPACE.PROJECTNAME.api.session
  (:require [clojure.spec.alpha :as s]
            [integrant.core :as ig]
            [ring.util.codec :as codec]
            [PROJECTNAMESPACE.PROJECTNAME.validation :refer [url?]]
            [net.danielcompton.defn-spec-alpha :as dfs]
            [ring.middleware.cookies :as ring.mw.cookies]
            [ring.middleware.session :as ring.mw.session]
            [ring.middleware.session.store :as session.store]
            [PROJECTNAMESPACE.PROJECTNAME.api.utils.time :as utils.time]
            [PROJECTNAMESPACE.PROJECTNAME.crux-session-store :refer [crux-store]]
            [spell-spec.alpha :as spell]))

;TODO spec for node?
(s/def ::node some?)

(s/def ::store
  (spell/keys :req-un [::node]))

(s/def ::secure boolean?)

(s/def ::http-only boolean?)

(s/def ::same-site #{:lax :strict})

(s/def ::cookie-attrs
  (spell/keys :req-un [::secure ::http-only ::same-site]))

(s/def :session/data any?)

(defmethod ig/pre-init-spec ::opts
  [_]
  (spell/keys :req-un [::store ::cookie-attrs]))

(defmethod ig/init-key ::opts
  [_ {:keys [store] :as m}]
  (assoc m :store (crux-store store)))

(def cookie :PROJECTNAME_sid)

(def expired-cookie {:value 0 :max-age -1 :path "/"})

(defn encode-cookies [cookies]
  (#'ring.mw.cookies/write-cookies cookies codec/form-encode))

(def cookie-expiry-secs
  (* 60 60 24 7))

(s/def ::expires-at inst?)

(s/def ::set-cookie (s/or :encoded (s/coll-of string?)
                          :unencoded (s/keys)))
(s/def ::url url?)

(s/def ::cookie-response
  (s/keys :req-un [::set-cookie ::expires-at ::url]))

(defn PROJECTNAME-cookie
  [response customer-id {:keys [session-opts]}]
  (prn (:store session-opts))
  (let [session {:login customer-id :account-type :customer}
        session-store (:store session-opts)
        session-key (session.store/write-session session-store nil session)
        cookie-attrs (assoc (:cookie-attrs session-opts)
                            :value session-key
                            :max-age cookie-expiry-secs)]
    (assoc-in response [:cookies cookie] cookie-attrs)))

(defn wrap-session
  [handler & [{:keys [store cookie-attrs]}]]
  (let [opts (cond-> {:cookie-attrs cookie-attrs
                      :cookie-name (name cookie)}
               store (assoc :store store))]
    (ring.mw.session/wrap-session handler opts)))

(dfs/defn update!
  "Update the session data.
     * Merge updates with existing session data
     * Merge the result with possible previous updates made while processing this request"
  [response current-session-data session-updates :- :session/data]
  (->> (merge current-session-data session-updates)
       (update response :session merge)))

(defn expire!
  "Delete the session from the store and expire the session cookie."
  [response]
  (assoc response
         :session nil
         :cookies {cookie expired-cookie}))
