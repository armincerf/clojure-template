(ns PROJECTNAMESPACE.PROJECTNAME.api.fixtures
  (:require [integrant.core :as ig]
            edge.system))

(def ^:dynamic *system*)

(defn start-test-system []
  (-> (edge.system/system-config {:profile :test})
      (ig/init)))

(defn with-system [f]
  (binding [*system* (start-test-system)]
    (let [res (f)]
      (ig/halt! *system*)
      res)))
