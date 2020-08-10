(ns PROJECTNAMESPACE.PROJECTNAME.api.assets.model
  (:require [net.danielcompton.defn-spec-alpha :as dfs]
            [clojure.tools.logging :as log]
            [clojure.spec.alpha :as s]
            [PROJECTNAMESPACE.PROJECTNAME.api.db :as db]))

(defn find-all
  [db]
  (def db db)
  (db/query db '{:find [(eql/project ?e [*])]
                 :where [[?e :asset/type]]}))

(dfs/defn insert!
  [db
   data :- :asset/int]
  (db/insert! db data))

(dfs/defn find-by-id
  [db asset-id :- :asset/id]
  (log/info "finding " asset-id)
  (db/lookup-vector db asset-id))

(dfs/defn delete-by-id!
  [db asset-id :- :asset/id]
  (db/delete-by-id! db [asset-id]))

(dfs/defn update-by-id!
  [db
   asset-id :- :asset/id
   data :- :asset/int]
  (db/entity-update! db asset-id data))
