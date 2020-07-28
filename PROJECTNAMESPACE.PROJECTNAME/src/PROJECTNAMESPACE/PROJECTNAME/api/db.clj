(ns PROJECTNAMESPACE.PROJECTNAME.api.db
  (:require [crux.api :as crux]
            [clojure.tools.logging :as log]
            [PROJECTNAMESPACE.PROJECTNAME.api.errors :as errors]
            [clojure.spec.alpha :as s]))

(defn delete-by-id
  "Delete documents associated with given crux ids"
  [node crux-ids]
  (crux/submit-tx node (for [crux-id crux-ids]
                      [:crux.tx/delete crux-id])))

(defn query
  [node query]
  (crux/q (crux/db node) query))

(defn insert!
  "Inserts data into crux, data can be either a map or a sequence of maps.
   Optionally takes a async? boolean, if true the function will immediately
  return the data without waiting for crux to confirm the transaction. Async? is
  false by default"
  ([system data] (insert! system data false))
  ([system data async?]
   (log/info "inserting " data)
   (if (seq data)
     (do (crux/submit-tx
          system
          (if (map? data)
            [[:crux.tx/put
              data]]
            (vec (for [item data]
                   [:crux.tx/put item]))))
         (when-not async? (crux/sync system))
         data)
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
  (if (vector? eid)
    (let [[index value] eid]
      (recur
       node
       (ffirst
        (crux.api/q node
                    {:find ['?e]
                     :where [['?e index value]]}))))
    (crux.api/entity node eid)))

(defn entity-update
  [node entity-id new-attrs]
  (let [entity-prev-value (crux/entity (crux/db node) entity-id)]
    (insert! crux (merge entity-prev-value new-attrs))))

