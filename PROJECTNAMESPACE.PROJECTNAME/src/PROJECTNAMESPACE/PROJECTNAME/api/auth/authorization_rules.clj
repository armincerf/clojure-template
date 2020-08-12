(ns PROJECTNAMESPACE.PROJECTNAME.api.auth.authorization-rules
  "Handler functions for authorization via buddy access rules. See here:
  https://funcool.github.io/buddy-auth/latest/#access-rules"
  (:require [buddy.auth :refer [authenticated?]]
            [buddy.auth.accessrules :refer [error success]]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [ring.util.response :as ring.util]
            [PROJECTNAMESPACE.PROJECTNAME.api.request-identity :as req-identity]))

(defn open
  "Any request is authorized."
  [_request]
  (success))

(defn inaccessible
  "No request is authorized."
  [_request]
  (error {:name ::inaccessible}))

(defn admin-role
  "Request identity must be an admin."
  [request]
  (if (and (authenticated? request)
           (req-identity/admin? request))
    (success)
    (error {:name ::customer-role
            :type ::role})))

(defn any-role
  "Request must be authenticated."
  [request]
  (if (authenticated? request)
    (success)
    (error {:name ::any-role
            :type ::role})))

(defn customer-role
  "Request identity must be a customer account."
  [request]
  (if (and (authenticated? request)
           (req-identity/customer? request))
    (success)
    (error {:name ::customer-role
            :type ::role})))

(defn access
  "Takes a predicate and returns a rule which asserts that the request must
  satisfy the predicate."
  [predicate]
  (fn [request]
    (if (predicate request)
      (success)
      (error {:name ::access}))))

(def owns
  "Uses the properties key in route data to determine if resource is owned by
  current user"
  (access (fn [request]
            (= (req-identity/account-id request)
               (get-in request [:properties :creator-id])))))

(defn owns-by
  [properties-accessor]
  (access (fn [request]
            (= (req-identity/account-id request)
               (properties-accessor (:properties request))))))

(def customer-owns
  "Uses the `customer-id` key in the properties object to determine if resource
  concerns the current user"
  (access (fn [request]
            (= (req-identity/account-id request)
               (get-in request [:properties :customer-id])))))
