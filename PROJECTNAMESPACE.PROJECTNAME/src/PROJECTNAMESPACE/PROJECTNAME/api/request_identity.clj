(ns PROJECTNAMESPACE.PROJECTNAME.api.request-identity)

(defn account-id [req]
  (get-in req [:identity :id]))

(defn account-type
  [req]
  (get-in req [:identity :account-type]))

(defn authentication-type
  [req]
  (get-in req [:identity :authentication-context]))

(defn customer?
  [req]
  (= :customer (account-type req)))

(defn admin?
  [req]
  (= :admin (account-type req)))
