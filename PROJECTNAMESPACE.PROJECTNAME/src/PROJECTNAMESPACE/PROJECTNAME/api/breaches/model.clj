(ns PROJECTNAMESPACE.PROJECTNAME.api.breaches.model
  (:require [net.danielcompton.defn-spec-alpha :as dfs]
            [clojure.tools.logging :as log]
            [clojure.spec.alpha :as s]
            [PROJECTNAMESPACE.PROJECTNAME.api.db :as db]
            [PROJECTNAMESPACE.PROJECTNAME.api.ids :as ids]))

(defn find-all
  [db]
  (def db db)
  (db/query db '{:find [(eql/project ?e [*])]
                   :where [[?e :breach/type]]}))

(dfs/defn insert!
  [node
   breach :- :breach/int
   asset-id]
  (db/insert! node (merge
                    {:crux.db/id (or (:crux.db/id breach)
                                     (ids/breach))
                     :breach/assets #{asset-id}}
                    breach)))

(dfs/defn find-by-id
  [db asset-id :- :asset/id]
  (log/info "finding " asset-id)
  (def db db)
  (def asset-id asset-id)
  (first
   (db/query-with-last-updated
    db
    {:find '[(eql/project ?breach [:breach/data :crux.db/id])]
     :where '[[?breach :breach/assets id]]
     :args [{'id asset-id}]})))

(dfs/defn delete-by-id!
  [node breach-id :- :breach/id]
  (db/delete-by-id! node [breach-id]))

(dfs/defn update-by-id!
  [node
   breach-id :- :breach/id
   data :- :breach/int]
  (db/entity-update! node breach-id data))
