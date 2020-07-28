(ns PROJECTNAMESPACE.PROJECTNAME.api.seed
  (:require [clojure.spec.alpha :as s]
            [cheshire.core :as json]
            [integrant.core :as ig]
            [spell-spec.alpha :as spell]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [PROJECTNAMESPACE.PROJECTNAME.api.db :as db]
            [PROJECTNAMESPACE.PROJECTNAME.api.ids :as ids]
            [clojure.string :as str]))

(s/def ::seed-file-directory string?)

(defmethod ig/pre-init-spec :PROJECTNAMESPACE.PROJECTNAME.api/seed
  [_]
  (spell/keys :req-un [::seed-file-directory]))

(defmethod ig/init-key :PROJECTNAMESPACE.PROJECTNAME.api/seed
  [_ {:keys [node seed-file-directory]}]
  (let [remove-hidden (fn [file-seq] (remove #(.isHidden %) file-seq))
        seed-files (-> (io/resource "seeds")
                       clojure.java.io/file
                       file-seq
                       remove-hidden)
        documents (flatten
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
                        doc
                        {:crux.db/id (ids/gen-id doc-type)}
                        (case doc-type
                          "cust" {:customer/email (str (str/replace (:name doc) #" |'" "-") "@fakemail.com")}
                          nil)))))]
    (db/insert! node documents)))

