(ns PROJECTNAMESPACE.PROJECTNAME.api.seed
  (:require [clojure.spec.alpha :as s]
            [cheshire.core :as json]
            [integrant.core :as ig]
            [spell-spec.alpha :as spell]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [PROJECTNAMESPACE.PROJECTNAME.api.db :as db]
            [PROJECTNAMESPACE.PROJECTNAME.api.ids :as ids]
            [medley.core :as medley]
            [clojure.string :as str]))

(s/def ::seed-file-directory string?)

(defmethod ig/pre-init-spec :PROJECTNAMESPACE.PROJECTNAME.api/seed
  [_]
  (spell/keys :req-un [::seed-file-directory]))

(defn- namespace-keywords
  "Given a map, add the given namespace to all non namespaced keywords"
  [obj ns]
  (medley/map-keys
   #(when (nil? (namespace %))
      (keyword ns (name %)))
   obj))

(defmethod ig/init-key :PROJECTNAMESPACE.PROJECTNAME.api/seed
  [_ {:keys [node seed-file-directory]}]
  (let [remove-hidden (fn [file-seq] (remove #(.isHidden %) file-seq))
        seed-files (-> (io/resource "seeds")
                       clojure.java.io/file
                       file-seq
                       remove-hidden)
        documents-from-seed-files
        (flatten
         (for [file (drop 1 seed-files)]
           (for [doc (json/parse-string
                      (slurp file)
                      (fn [k]
                        (keyword (.toLowerCase k))))
                 :let [doc-type (-> file
                                    .getName
                                    (str/split #"\.")
                                    first)]]
             (merge
              (namespace-keywords doc doc-type)
              {:crux.db/id (ids/gen-id doc-type)}
              (case doc-type
                "customer" {:customer/email
                            (some-> doc
                                    :name
                                    (str/replace #" |'" "-")
                                    (str "@fakemail.com"))}
                nil)))))
        customers (filter
                   #(ids/customer? (:crux.db/id %))
                   documents-from-seed-files)
        assets
        (for [customer customers
              :let [asset-type (rand-nth [:email :phone :password])]]
          {:crux.db/id (ids/asset)
           :asset/customer (:crux.db/id customer)
           :asset/type asset-type
           :asset/data (case asset-type
                         :email (:customer/email customer)
                         :phone (str (rand-int 99999999))
                         :password "bad password")
           :asset/name "User defined name for data"
           :asset/description "User defined description for data, could potentially be
           quite long although probably not."})]
    (db/drop-db! node)
    (db/insert! node (concat documents-from-seed-files assets))
    ))

