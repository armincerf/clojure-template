(ns PROJECTNAMESPACE.PROJECTNAME.validation
  (:refer-clojure :exclude [uri?])
  #?@
   (:clj
    [(:require
      [cemerick.url :as url]
      [clojure.string :as string]
      [vlad.core :as vlad :refer [attr chain join present Validation]])]
    :cljs
    [(:require
      [clojure.string :as string]
      [goog.date.Interval :as Interval]
      [vlad.core :as vlad :refer [attr chain join present Validation]])]))

;; Predicates are simple functions that take some data and return a boolean
;; value. They're ideal for use as validators and so `Predicate` exists to make
;; wrapping them up easy. All that's needed to turn a predicate into a
;; validator is an error message.
(defrecord Predicate [predicate information]
  Validation
  (validate [{:keys [predicate information]} data]
    (if (predicate data)
      [(merge information (when data {:invalid-data data}))]
      [])))

(defn predicate
  "A predicate automatically adds a :invalid-data key, with the data the failed
   the validation. This is useful for including the failed data in your messages.
   Examples:
    (predicate #(> size (count %))
               {:type ::length-over :size size})
    (predicate [:user :password]
               #(> size (count %))
               {:type ::length-over :size size})"
  ([pred information]
   (Predicate. pred information))
  ([selector pred information]
   (attr selector (predicate pred information))))

(defn or-empty-string
  "Return the original value or an empty string when the value is nil."
  [value]
  (or value ""))

(defn or-zero
  "Return the original value or 0 when the value is nil."
  [value]
  (or value "0"))

(defn or-empty-vec
  "Return the original value or an emtpy vector when the value is nil."
  [value]
  (or value []))

(defn check-nil-then-predicate
  "Check if the value is nil, then apply the predicate.
   This is useful only for mandatory fields."
  [value predicate]
  (if (nil? value)
    false
    (predicate value)))

(defn optional-then-predicate
  "Apply the predicate when value is not nil, otherwise return true.
   This is useful for optional fields."
  [value predicate]
  (if (nil? value)
    true
    (predicate value)))

(defn valid-date?
  [s]
  (check-nil-then-predicate
   s
   (fn [s]
     (boolean (re-matches #"\d\d/\d\d/\d\d\d\d" s)))))

(defn valid-iso-8601-date?
  [s]
  (check-nil-then-predicate
   s
   (fn [s]
     (boolean (re-matches #"\d\d\d\d-\d\d-\d\d" s)))))

(defn valid-optional-date?
  [s]
  (optional-then-predicate
   s
   (fn [s]
     (boolean (re-matches #"\d\d/\d\d/\d\d\d\d" s)))))

(defn valid-amount?
  [s]
  (check-nil-then-predicate
   s
   (fn [s]
     (or (= (count s) 1)
         (boolean (re-matches #"-?\d+\,?\d+" s))))))

(defn not-empty?
  [value]
  (check-nil-then-predicate
   value
   (fn [v]
     (if (seqable? v)
       (not (empty? v))
       true))))

(defrecord Optional [validator]
  Validation
  (validate [_ data]
    (if (string/blank? data)
      []
      (vlad/validate validator data))))

(defn optional
  "Defines an optional predicate, only validates if the field is not clojure.string/blank?"
  [validator]
  (->Optional validator))

(defn oauth-authz-code?
  "True if `s` is a string representing an OAuth authorization code."
  [s]
  (and (string? s)
       (= s (re-find #"[A-Z0-9]{32}$" s))))

(defn oauth-token?
  "True if `s` is a string representing an OAuth access or refresh token."
  [s]
  (and (string? s)
       (or
        ;; backwards compatibility
        (= s (re-find #"[A-Z0-9]{32}$" s))
        (= s (re-find #"[-a-zA-Z0-9._~+/]+=*" s)))))

(defn url? [s]
  #?(:clj (try (url/url s) true (catch Exception _ false))
     ;; Cemerick.url doesn't throw an exception when given a malformed URL in
     ;; cljs so we need to use regex
     :cljs (re-seq
            #"(?i)^(?:(?:https?|ftp)://)(?:\S+(?::\S*)?@)?(?:(?!(?:10|127)(?:\.\d{1,3}){3})(?!(?:169\.254|192\.168)(?:\.\d{1,3}){2})(?!172\.(?:1[6-9]|2\d|3[0-1])(?:\.\d{1,3}){2})(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])(?:\.(?:1?\d{1,2}|2[0-4]\d|25[0-5])){2}(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))|(?:(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)(?:\.(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)*(?:\.(?:[a-z\u00a1-\uffff]{2,}))\.?)(?::\d{2,5})?(?:[/?#]\S*)?$"
            s)))

(defn uri?
  [s]
  (and (string? s)
       #?(:clj (try (java.net.URI. s)
                    true
                    (catch Exception _ false))
          :cljs (= (string/replace (js/encodeURI s) "%25" "%")
                   s))))

(defn email? [s]
  (and (string? s)
       (some? (re-matches #".+@.+\..+" s))))

#?(:cljs
   (defn iso8601-interval
     []
     (predicate (complement #(Interval/fromIsoString %))
                {:type ::iso-8601-interval})))

(defn includes-uppercase? [s]
  (some? (re-find #"[A-Z]" s)))

(defn includes-lowercase? [s]
  (some? (re-find #"[a-z]" s)))

(defn includes-letter? [s]
  (some? (re-find #"[a-zA-Z]" s)))

(defn includes-digit? [s]
  (some? (re-find #"[0-9]" s)))

(defn includes-symbol? [s]
  (some? (some (set (map #?(:clj int :cljs #(.charCodeAt % 0)) s))
               ;; symbol ascii codes
               (vec (concat (range 32 48)
                            (range 58 65)
                            (range 91 97)
                            (range 123 127))))))

(def restricted-char-codes
  (set (range 0 32)))

(defn restricted-char?
  [character]
  (let [code #?(:clj (int character) :cljs (.charCodeAt character 0))]
    (or (< 126 code)
        (restricted-char-codes code))))

(defn valid-charset? [s]
  (not (some restricted-char? s)))

(def english-field-names
  {[:password] "Password"})

(def password-validation
  (attr [:password]
        (chain (present)
               (vlad/length-in 7 161)
               (predicate (complement includes-letter?) {:type :pw-includes-letter})
               (predicate (complement includes-digit?) {:type :pw-includes-digit})
               (predicate (complement valid-charset?) {:type :pw-valid-charset}))))

(def email-pred (predicate (complement email?) {:type :has-at-sign}))

(def valid-email? (attr ["email"]
                        (chain
                         (present)
                         email-pred)))

(def not-nil? (predicate nil? {:type :not-present}))

(def email-validation
  (attr [:email]
        (chain
         (present)
         email-pred)))

(def url-pred (predicate (complement url?) {:type :valid-url}))

(def valid-url? (attr [:url] url-pred))

(def webhook-validation
  (attr [:url]
        (chain
         (present)
         url-pred)))

(defn validate-password
  [password]
  (-> (vlad/validate password-validation {:password password})
      (vlad/assign-names english-field-names)
      (vlad/translate-errors vlad/english-translation)
      (get [:password])
      (first)))

(defn validate-password!

  [password]
  (-> (vlad/validate password-validation {:password password})
      (vlad/assign-names english-field-names)
      (vlad/translate-errors vlad/english-translation)
      (get [:password])
      (first)))

(def validate-pos-int
  (predicate (complement pos-int?) {:type :pos-int}))

(def validate-iso8601-date
  (predicate (complement valid-iso-8601-date?) {:type :valid-date}))

(def login-validations
  (join email-validation password-validation))

(def signup-validations
  (join email-validation password-validation))

(def userdata-validation
  (join (attr [:name] (present))
        (attr [:account] (predicate (complement pos-int?) {:type :pos-int}))
        (attr [:description] (present))))

(defmethod vlad.core/english-translation :pw-includes-lower
  [{:keys [name]}]
  (str name " must contain at least one lowercase letter."))

(defmethod vlad.core/english-translation :pw-includes-upper
  [{:keys [name]}]
  (str name " must contain at least one uppercase letter."))

(defmethod vlad.core/english-translation :pw-includes-sym
  [{:keys [name]}]
  (str name " must contain at least one symbol."))

(defmethod vlad.core/english-translation :pw-includes-digit
  [{:keys [name]}]
  (str name " must contain at least one number."))

(defmethod vlad.core/english-translation :pw-includes-letter
  [{:keys [name]}]
  (str name " must contain at least one letter."))

(defmethod vlad.core/english-translation :pw-valid-charset
  [{:keys [name invalid-data]}]
  (str name " contains invalid characters: "
       (->> invalid-data
            (filterv restricted-char?)
            (set)
            (string/join ", "))))

(defmethod vlad.core/english-translation :pos-int
  [_]
  "Must be a positive number")

(defmethod vlad.core/english-translation :valid-date
  [_]
  "Must be a valid ISO8601 date")

(defmethod vlad.core/english-translation :not-present
  [_]
  "Must be specified")

(defmethod vlad.core/english-translation :has-at-sign
  [{:keys [invalid-data]}]
  (str invalid-data " is not a valid email."))

(defmethod vlad.core/english-translation :valid-url
  [{:keys [invalid-data]}]
  (cond
    (and (not (string/starts-with? invalid-data "https://"))
         (not (string/starts-with? invalid-data "http://")))
    "URL must start with 'http://' or 'https://'"
    :else
    (str invalid-data " must be a valid URL.")))

(defmethod vlad.core/english-translation ::iso-8601-interval
  [_]
  "Must conform to ISO 8601 intervals")
