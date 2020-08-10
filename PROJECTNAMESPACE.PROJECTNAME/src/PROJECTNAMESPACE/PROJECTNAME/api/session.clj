(ns PROJECTNAMESPACE.PROJECTNAME.api.session
  (:require [clojure.spec.alpha :as s]
            [integrant.core :as ig]
            [net.danielcompton.defn-spec-alpha :as dfs]
            [ring.middleware.session :as ring.mw.session]
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

(defmethod ig/pre-init-spec ::opts
  [_]
  (spell/keys :req-un [::store ::cookie-attrs]))

(defmethod ig/init-key ::opts
  [_ {:keys [store] :as m}]
  ;(assoc m :store (crux-store (:node store)))
  m
  )

(def cookie :PROJECTNAME_sid)

(def expired-cookie {:value 0 :max-age -1 :path "/"})

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
