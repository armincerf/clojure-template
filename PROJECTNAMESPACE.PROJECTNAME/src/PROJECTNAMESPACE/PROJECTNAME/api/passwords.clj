(ns PROJECTNAMESPACE.PROJECTNAME.api.passwords
  (:refer-clojure :exclude [hash])
  (:require [buddy.hashers :as hashers]
            [clojure.string :as str]
            [integrant.core :as ig]
            [PROJECTNAMESPACE.PROJECTNAME.api.errors :as errors]
            [PROJECTNAMESPACE.PROJECTNAME.validation :as validation]))

(defprotocol PasswordHasher
  (hash [this password])
  (validate-and-hash [this password])
  (match? [this provided-password encrypted-password]))

(defn -hash
  [password]
  (hashers/derive password {:alg :scrypt}))

(defn -match?
  [provided-password encrypted-password]
  (hashers/check provided-password encrypted-password))

(defn- validate
  [password]
  (if-let [err-msg (validation/validate-password password)]
    (throw (errors/exception :account/failed-password-policy {:message err-msg}))
    password))

(defrecord BuddyHasher []
  PasswordHasher
  (hash [_ password]
    (-hash password))
  (validate-and-hash [this password]
    (-> password
        validate
        -hash))
  (match? [_ provided-password encrypted-password]
    (-match? provided-password encrypted-password)))

(defmethod ig/init-key ::hasher
  [_ _]
  (->BuddyHasher))

(def symbols
  (vec (concat (range 33 48)
               (range 58 65)
               (range 91 97)
               (range 123 127))))

(defn rand-password
  []
  (->> (concat
        ;; uppercase letters
        (take 4 (shuffle (map char (range 65 91))))
        ;; lowercase letters
        (take 4 (shuffle (map char (range 97 123))))
        ;; some symbols
        (take 1 (shuffle (map char symbols)))
        ;; numbers
        (take 1 (shuffle (map char (range 48 58)))))
       (shuffle)
       (str/join)))
