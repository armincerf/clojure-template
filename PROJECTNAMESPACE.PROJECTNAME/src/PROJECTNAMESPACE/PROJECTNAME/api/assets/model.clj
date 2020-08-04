(ns PROJECTNAMESPACE.PROJECTNAME.api.assets.model
  (:require [net.danielcompton.defn-spec-alpha :as dfs]
            [clojure.tools.logging :as log]
            [clojure.spec.alpha :as s]
            [PROJECTNAMESPACE.PROJECTNAME.api.db :as db]))

(defn find-all
  [node]
  (def node node)
  (db/query node '{:find [?e]
                   :where [[?e :asset/type]]
                   :full-results? true}))

(dfs/defn insert!
  [node
   data :- :asset/int]
  (prn "adding new asset" (:crux.db/id data))
  (db/insert! node data))

(dfs/defn find-by-id
  [node asset-id :- :asset/id]
  (log/info "finding " asset-id)
  (db/lookup-vector node asset-id))

(dfs/defn delete-by-id
  [node asset-id :- :asset/id]
  (db/delete-by-id node [asset-id]))

(dfs/defn update-by-id
  [node
   asset-id :- :asset/id
   data :- :asset/int]
  (db/entity-update node asset-id data))
