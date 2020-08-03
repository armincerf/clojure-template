(ns PROJECTNAMESPACE.PROJECTNAME.common
  (:require [clojure.walk :as walk]
            [clojure.string :as str]
            [medley.core :as medley]))

(defn remove-kw-ns
  [k]
  (if (keyword? k)
    (keyword (name k))
    k))

(defn add-external-id
  "Adds an id key with the crux id which won't be filtered out when sent to the frontend"
  [obj]
  (assoc obj :id (:crux.db/id obj)))

(defn id-key
  "Takes a namespaced keyword (or stringified ns keyword) and returns a string of
  the keyword with the namespace removed. E.g :foo/bar becomes 'bar'"
  [id]
  (some-> id
          str
          keyword
          name))

(defn keyword->string
  "Takes a namespaced keyword and returns a string of the keyword that can be turned back into a keyword later"
  [k]
  (some-> k
          str
          (subs 1)))

(defn loading-component
  [title]
  [:<>
   [:h1 title]
   [:div.fill-loader.fill-loader--v6
    {:role "alert"}
    [:p.fill-loader__label "Content is loading..."]
    [:div.fill-loader__grid
     {:aria-hidden "true"}
     [:div.fill-loader__bar
      [:div.fill-loader__base]
      [:div.fill-loader__fill.fill-loader__fill--1st]]
     [:div.fill-loader__bar
      [:div.fill-loader__base]
      [:div.fill-loader__fill.fill-loader__fill--2nd]]
     [:div.fill-loader__bar
      [:div.fill-loader__base]
      [:div.fill-loader__fill.fill-loader__fill--3rd]]]]])

(defn find-by-id
  "Given a list of maps, find the first map where the :id key equals the given id"
  [data id]
  (medley/find-first #(= id (:id %)) data))
