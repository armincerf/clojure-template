(ns PROJECTNAMESPACE.PROJECTNAME.common
  (:require [clojure.walk :as walk]
            [clojure.string :as str]))

(defn remove-kw-ns
  [k]
  (if (keyword? k)
    (keyword (name k))
    k))

(defn prep-map
  "Transforms a map into one which will be sent to the frontend. This involves
  stripping the namespace from keys"
  [data]
  (let [transform-map (fn [form]
                        (if (map? form)
                          (reduce-kv
                           (fn [acc k v]
                             (assoc acc (remove-kw-ns k) v)) {} form)
                          form))]
    (walk/postwalk transform-map data)))

(defn id-key
  "Takes a namespaced keyword (or stringified ns keyword) and returns a string of
  the keyword with the namespace removed. E.g :foo/bar becomes 'bar'"
  [id]
  (some-> id
          str
          keyword
          name))
