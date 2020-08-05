(ns PROJECTNAMESPACE.PROJECTNAME.api.db
  (:require [crux.api :as crux]
            [clojure.tools.logging :as log]
            [PROJECTNAMESPACE.PROJECTNAME.api.errors :as errors]
            [clojure.spec.alpha :as s]))

(defn delete-by-id
  "Delete documents associated with given crux ids"
  [node crux-ids]
  (crux/submit-tx
   node
   (for [crux-id crux-ids]
     [:crux.tx/delete crux-id])))

(defn query
  [node query]
  (let [result (crux/q (crux/db node) query)]
    (if (and (seq result) (= 1 (count (:find query))))
      (map first result)
      result)))

(defn query-with-last-updated
  "Executes the query and assocs the last updated tx-time to the result"
  [node q]
  (let [results (query node q)]
    (for [item results]
      (do
        (assoc item
               :tx-time
               (:crux.db/valid-time
                (crux/entity-tx
                 (crux/db node) (:crux.db/id item))))))))

(defn insert!
  "Inserts data into crux, data can be either a map or a sequence of maps.
   Optionally takes a async? boolean, if true the function will immediately
  return the data without waiting for crux to confirm the transaction. Async? is
  false by default"
  ([system data] (insert! system data false))
  ([system data async?]
   (if (seq data)
     (let [{:keys [crux.tx/tx-time]}
           (crux/submit-tx
            system
            (if (map? data)
              [[:crux.tx/put
                data]]
              (vec (for [item data]
                     [:crux.tx/put item]))))]
       (when-not async? (crux/sync system))
       (assoc data :tx-time tx-time))
     (log/error "Not transacting as data is not valid" {:data data}))))

(defn lookup-vector
  "Queries crux for a document with either a given key, or a given eid vector
  e.g. [:unique-key value]"
  [node eid]
  (def node node)
  (def eid eid)
  (log/info "looking up " eid)
  (when-not (or (vector? eid)
                (s/valid? :crux.db/id eid))
    (errors/exception :db/invalid-lookup {:eid eid}))
  (let [db (crux/db node)]
    (if (vector? eid)
      (let [[index value] eid]
        (recur
         node
         (ffirst
          (crux/q db
                  {:find ['?e]
                   :where [['?e index value]]}))))
      (crux/entity db eid))))

(defn entity-update
  [node entity-id new-attrs]
  (let [entity-prev-value (crux/entity (crux/db node) entity-id)]
    (log/info "inserting " (merge entity-prev-value new-attrs))
    (insert! node (merge entity-prev-value new-attrs))))

(defn all-ids
  [node]
  (query node '{:find [?e]
                :where [[?e :crux.db/id]]}))

(defn drop-db!
  "WARNING deletes every item in the db. Although as this doesn't use evict the
  data will still exist in the history."
  [node]
  (crux/submit-tx
   node
   (for [id (all-ids node)]
     [:crux.tx/delete id])))
