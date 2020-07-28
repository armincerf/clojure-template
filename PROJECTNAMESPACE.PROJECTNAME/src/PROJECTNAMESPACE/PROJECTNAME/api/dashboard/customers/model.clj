(ns PROJECTNAMESPACE.PROJECTNAME.api.dashboard.customers.model
  (:require [net.danielcompton.defn-spec-alpha :as dfs]
            [PROJECTNAMESPACE.PROJECTNAME.api.db :as db]))

(defn find-all
  [node]
  (def node node)
  (db/query node '{:find [?e]
                   :where [[?e :customer/email]]
                   :full-results? true}))

(dfs/defn find-by-id
  [node customer-id :- :customer/id]
  (db/lookup-vector node customer-id))

(dfs/defn delete-by-id
  [node customer-id :- :customer/id]
  (db/delete-by-id node [customer-id]))
