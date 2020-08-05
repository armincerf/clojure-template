(ns PROJECTNAMESPACE.PROJECTNAME.api.breaches.model
  (:require [net.danielcompton.defn-spec-alpha :as dfs]
            [clojure.tools.logging :as log]
            [clojure.spec.alpha :as s]
            [PROJECTNAMESPACE.PROJECTNAME.api.db :as db]
            [PROJECTNAMESPACE.PROJECTNAME.api.ids :as ids]))

(defn find-all
  [node]
  (def node node)
  (db/query node '{:find [(eql/project ?e [*])]
                   :where [[?e :breach/type]]}))

(dfs/defn insert!
  [node
   data :- :breach/int
   asset-id]
  (prn "adding new breach" {:crux.db/id (ids/breach)
                            :breach/data data})
  (db/insert! node {:crux.db/id (ids/breach)
                    :breach/assets #{asset-id}
                    :breach/data data}))

(dfs/defn find-by-id
  [node asset-id :- :asset/id]
  (log/info "finding " asset-id)
  (first (db/query node {:find '[(eql/project ?breach [:breach/data])]
                         :where '[[?breach :breach/assets id]]
                         :args [{'id asset-id}]})))

(dfs/defn delete-by-id
  [node breach-id :- :breach/id]
  (db/delete-by-id node [breach-id]))

(dfs/defn update-by-id
  [node
   breach-id :- :breach/id
   data :- :breach/int]
  (db/entity-update node breach-id data))
