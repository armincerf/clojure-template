(ns PROJECTNAMESPACE.PROJECTNAME.api.spec
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as str]
            [spec-tools.core :as st]))

;; Utils

(s/def ::non-blank-string
  (st/spec {:description "Non-blank string"
            :spec (s/and string? (complement str/blank?))}))

(defn non-empty-string-alphanumeric
  "Generator for non-empty alphanumeric strings"
  []
  (gen/such-that #(not= "" %)
                 (gen/string-alphanumeric)))

(defn string-of-length-gen [n]
  (gen/fmap #(-> % str/join str/lower-case)
            (gen/vector (gen/char-alphanumeric) n)))

(s/def ::origin string?)

(s/def ::middleware
  (st/spec {:description "Middleware"
            :spec (s/* (s/alt :mw keyword?
                              :mw-with-ref (s/tuple keyword? some?)))}))

(s/def ::port
  (st/spec {:description "Port"
            :spec pos-int?}))

(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(s/def ::email (s/and string? #(re-matches email-regex %)))
