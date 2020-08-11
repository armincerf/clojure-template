(ns PROJECTNAMESPACE.PROJECTNAME.crux-session-store
  (:require [clojure.data :as data]
            [crux.api :as crux]
            [PROJECTNAMESPACE.PROJECTNAME.api.db :as db]
            [ring.middleware.session.store :as rs]))

(defn key->eid [db key-attr key]
  (first
   (crux/q db {:find '[?eid]
               :where '[[?eid attr k]]
               :args [{'attr key-attr
                       'k key}]})))

(defn str->uuid [s]
  (when s
    (try (java.util.UUID/fromString s)
         (catch java.lang.IllegalArgumentException e nil))))

(deftype CruxStore [node key-attr auto-key-change?]
  rs/SessionStore
  (read-session [_ key]
    (prn "hhi?")
    (let [uuid-key (str->uuid key)]
      (prn "read" (when uuid-key
                    (let [db (crux/db node)]
                      (crux/entity db (key->eid db key-attr uuid-key)))))
      (when uuid-key
        (let [db (crux/db node)]
          (crux/entity db (key->eid db key-attr uuid-key))))))
  (write-session [_ key data]
    (prn "hhi?")
    (let [uuid-key (str->uuid key)
          _ (prn "write" key data)
          db (when uuid-key (crux/db node))
          eid (when uuid-key (key->eid db key-attr uuid-key))
          key-change? (or (not eid) auto-key-change?)
          uuid-key (if key-change?
                     (java.util.UUID/randomUUID) uuid-key)
          tx-data {uuid-key data}]
      (if eid
        (db/entity-update! node eid tx-data)
        (db/insert! node tx-data))
      (str uuid-key)))
  (delete-session [_ key]
    (prn "hhi?")
    (when-let [uuid-key (str->uuid key)]
      (when-let [eid (key->eid (crux/db node) key-attr uuid-key)]
        (db/delete-by-id! node [eid])))
    nil))

(defn crux-store [{:keys [node key-attr auto-key-change?]
                      :or {key-attr :session/key}}]
  (CruxStore. node key-attr auto-key-change?))
