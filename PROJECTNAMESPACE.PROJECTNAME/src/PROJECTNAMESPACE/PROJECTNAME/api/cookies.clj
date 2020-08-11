(ns PROJECTNAMESPACE.PROJECTNAME.api.cookies
  (:require [buddy.sign.jws :as jws]
            [clojure.tools.logging :as log]
            [ring.util.http-response :refer [get-header]])
  (:import clojure.lang.ExceptionInfo))

(def entitlements-cookie :PROJECTNAMESPACE.PROJECTNAME1)
(def expired-cookie {:value 0 :max-age -1 :path "/"})
(def secret "TODO")

(def one-day 86400)

(defn expire!
  "Expire the named cookie."
  [response cookie]
  (update response :cookies merge {cookie expired-cookie}))

(defn parse-jws-cookie
  "Returns parsed ID value or nil"
  [v secret]
  (try (let [s (some-> v
                       ^bytes (jws/unsign secret)
                       (String. "UTF-8"))]
         (when s
           (try (Integer/parseInt ^String s)
                (catch NumberFormatException _ s))))
       (catch ExceptionInfo e
         (log/error e "Error while unsigning cookie"))))

(defn entitlements-cookie-attrs [id secret]
  {:value (jws/sign (.getBytes (str id) "UTF-8") secret)
   :path "/"
   :max-age one-day
   ;:same-site :none
   :secure false ;;TODO We should (preferably) start using https in dev or at
                 ;;least make sure this is true in prod
   :http-only true})

(defn set-entitlements-cookie
  "Set the entitlements cookie for a consumer user on the response."
  [response id secret]
  (def response response)
  (let [{:keys [account-type]} (:session response)]
    (if (= :customer account-type)
      (assoc-in response [:cookies entitlements-cookie]
                (entitlements-cookie-attrs id secret))
      response)))
