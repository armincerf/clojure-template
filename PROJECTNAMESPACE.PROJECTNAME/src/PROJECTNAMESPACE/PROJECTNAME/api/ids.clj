(ns PROJECTNAMESPACE.PROJECTNAME.api.ids
  (:require [PROJECTNAMESPACE.PROJECTNAME.api.crypto :as crypto]))

(defn gen-id
  "Generates an opaque keyword ID with a human readable prefix.
  Heavily inspired by https://stripe.com/docs/api

  ```
  (gen-id \"customer\")
  => :cus/AAABa4cs1BICijqMPoVAOg
  ```

  The ID is intentionally opaque. Any internal details of the ID
  beyond the prefix, including length are subject to change at any time.

  This function is private. If you need to generate a new type of ID,
  create a new function below which uses this function. This is to prevent
  subtle mistypings of ID prefixes."
  [prefix]
  ;; Note: random-nonce has a time component to it, which will mean
  ;; that the first part of the IDs looks quite static, though it does
  ;; change slowly over time. This is useful for uniqueness, but
  ;; also means that our IDs can be sorted to get rough creation time.
  (keyword prefix (str "a" (crypto/random-string))))

;; Prefixes - short, unique string prepended to each id.

(def prefixes
  {:customer "customer"
   :breach "breach"
   :asset "asset"
   :session "sesh"})

(defn session [] (gen-id (:session prefixes)))
(defn customer [] (gen-id (:customer prefixes)))
(defn asset [] (gen-id (:asset prefixes)))
(defn breach [] (gen-id (:breach prefixes)))

(defn customer? [id] (= (namespace (keyword id)) (:customer prefixes)))
(defn session? [id] (= (namespace (keyword id)) (:session prefixes)))
(defn asset? [id] (= (namespace (keyword id)) (:asset prefixes)))
(defn breach? [id] (= (namespace (keyword id)) (:breach prefixes)))
