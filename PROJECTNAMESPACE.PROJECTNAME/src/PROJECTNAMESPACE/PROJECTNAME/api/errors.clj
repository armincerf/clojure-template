(ns PROJECTNAMESPACE.PROJECTNAME.api.errors
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [net.danielcompton.defn-spec-alpha :as dfs]
            [spell-spec.alpha :as spell]
            [stringer.core :as stringer]))

(s/def ::code keyword?)
(s/def ::info map?)

(dfs/defn exception
  "Creates a new ex-info properly formatted to later be expanded.
   Can be thrown or used to resolve existing deferred"
  ([code]
   (exception code {}))
  ([code :- ::code
    info :- ::info]
   (ex-info (name code)
            {:type ::exception
             :code code
             :info info})))

(dfs/defn code-not-found
  [registry
   code :- ::code
   info :- ::info]
  (log/warnf "Unknown error code %s provided, returning generic code." code)
  ;; if we intended to log info, it's likely invalid-request
  ;; since we're giving info back to user
  (if (seq info)
    (get registry :invalid-body)
    (get registry :internal-server-error)))

(defn code-name
  [code]
  (if (namespace code)
    (keyword (str (namespace code) "_" (name code)))
    code))

(defn error-context
  "Uses ex-info data and error registry to build full error context"
  [registry e]
  (let [{:keys [code info]} (ex-data e)
        code-definition (or (get registry code) (code-not-found registry code info))
        code (code-name code)]
    (-> code-definition
        (assoc :code code :info info)
        (update :message stringer/nrender info))))

(s/def ::registry-resource any?)

(defmethod ig/pre-init-spec :PROJECTNAMESPACE.PROJECTNAME.api/error-registry
  [_]
  (spell/keys :req-un [::registry-resource]))

(defmethod ig/init-key :PROJECTNAMESPACE.PROJECTNAME.api/error-registry
  [_ {:keys [registry-resource]}]
  (edn/read-string (slurp (io/resource registry-resource))))
