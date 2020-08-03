(ns PROJECTNAMESPACE.PROJECTNAME.common
  (:require [clojure.walk :as walk]
            [clojure.string :as str]))

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
