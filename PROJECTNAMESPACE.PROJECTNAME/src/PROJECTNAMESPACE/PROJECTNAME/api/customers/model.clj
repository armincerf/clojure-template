(ns PROJECTNAMESPACE.PROJECTNAME.api.customers.model
  (:require [net.danielcompton.defn-spec-alpha :as dfs]
            [PROJECTNAMESPACE.PROJECTNAME.api.db :as db]))

(defn find-all
  [db]
  (db/query db '{:find [?e]
                 :where [[?e :customer/email]]
                 :full-results? true}))

(dfs/defn find-by-id
  [db customer-id :- :customer/id]
  (db/lookup-vector db customer-id))

(dfs/defn delete-by-id!
  [db customer-id :- :customer/id]
  (db/delete-by-id! db [customer-id]))

(dfs/defn update-by-id!
  [db
   customer-id :- :customer/id
   data :- :customer/ext]
  (db/entity-update! db customer-id data))
