(ns PROJECTNAMESPACE.PROJECTNAME.api.utils
  (:require [ring.util.response :as ring.util]
            [medley.core :refer [dissoc-in]]
            [user-agent :as ua]))

(defn remove-header [resp header-name]
  (if-let [header (ring.util/find-header resp header-name)]
    (dissoc-in resp [:headers (key header)])
    resp))

(defn endpoint
  "Return the base endpoint of the request
   i.e. https://PROJECTNAMESPACE.PROJECTNAME.com"
  [request]
  (str (-> request :scheme name)
       "://"
       (get-in request [:headers "host"])))

(defn os-family
  [req]
  (let [user-agent (get-in req [:headers "user-agent"] "") ;; nil throws exception on parse
        parsed-ua (ua/parse user-agent)]
    (get-in parsed-ua [:os :family])))

(defn req->db
  "Extracts the db from the properties key of the req"
  [req]
  (-> req :properties :db))
