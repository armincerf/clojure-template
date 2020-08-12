(ns PROJECTNAMESPACE.PROJECTNAME.crux-session-store
  (:require [clojure.data :as data]
            [crux.api :as crux]
            [PROJECTNAMESPACE.PROJECTNAME.api.db :as db]
            [ring.middleware.session.store :as rs]))

(defn str->uuid [s]
  (when s
    (try (java.util.UUID/fromString s)
         (catch java.lang.IllegalArgumentException e nil))))

(deftype CruxStore [node key-change?]
  rs/SessionStore
  (read-session [_ key]
    (let [uuid-key (str->uuid key)]
      (when uuid-key
        (let [db (crux/db node)
              session (crux/entity db uuid-key)]
          (:value session)))))
  (write-session [_ key data]
    (let [uuid-key (or (str->uuid key)
                       (java.util.UUID/randomUUID))
          db (when uuid-key (crux/db node))
          uuid-key (if key-change?
                     (java.util.UUID/randomUUID) uuid-key)
          tx-data {:crux.db/id uuid-key
                   :value data}]
      (if uuid-key
        (db/entity-update! node uuid-key tx-data)
        (db/insert! node tx-data))
      (str uuid-key)))
  (delete-session [_ key]
    (when-let [uuid-key (str->uuid key)]
      (db/delete-by-id! node [uuid-key]))
    nil))

(defn crux-store [{:keys [node auto-key-change?]}]
  (CruxStore. node auto-key-change?))
