(ns PROJECTNAMESPACE.PROJECTNAME.api.admins.model
  (:require [net.danielcompton.defn-spec-alpha :as dfs]
            [PROJECTNAMESPACE.PROJECTNAME.api.db :as db]))

(defn find-all
  [db]
  (db/query db '{:find [?e]
                 :where [[?e :admin/email]]
                 :full-results? true}))

(dfs/defn find-by-id
  [db admin-id :- :admin/id]
  (db/lookup-vector db admin-id))

(dfs/defn delete-by-id!
  [db admin-id :- :admin/id]
  (db/delete-by-id! db [admin-id]))

(dfs/defn update-by-id!
  [db
   admin-id :- :admin/id
   data :- :admin/ext]
  (db/entity-update! db admin-id data))
