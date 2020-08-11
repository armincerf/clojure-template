(ns PROJECTNAMESPACE.PROJECTNAME.common
  (:require [clojure.walk :as walk]
            [clojure.string :as str]
            #?(:clj  [clojure.tools.logging :as log]
               :cljs [goog.log :as log])
            [clojure.spec.alpha :as s]
            [tick.alpha.api :as tick]
            [medley.core :as medley]))

(defn remove-kw-ns
  [k]
  (if (keyword? k)
    (keyword (name k))
    k))

(defn add-external-id
  "Adds an id key with the crux id which won't be filtered out when sent to the frontend"
  [obj]
  (assert (map? obj))
  (assoc obj :id (:crux.db/id obj)))

(defn id-key
  "Takes a namespaced keyword (or stringified ns keyword) and returns a string of
  the keyword with the namespace removed. E.g :foo/bar becomes 'bar'"
  [id]
  (some-> id
          str
          keyword
          name))

(defn keyword->string
  "Takes a namespaced keyword and returns a string of the keyword that can be turned back into a keyword later"
  [k]
  (some-> k
          str
          (subs 1)))

(defn keyword->readable-string
  "Takes a namespaced keyword and returns a string of the keyword that is
  capitalized and has dashes and dots replaced with spaces"
  [k]
  (some-> k
          keyword->string
          (str/replace #"\-" " ")
          (str/replace #"\." " ")
          str/capitalize))

(defn loading-component
  [title]
  [:<>
   [:h1 title]
   [:div.fill-loader.fill-loader--v6
    {:role "alert"}
    [:p.fill-loader__label "Content is loading..."]
    [:div.fill-loader__grid
     {:aria-hidden "true"}
     [:div.fill-loader__bar
      [:div.fill-loader__base]
      [:div.fill-loader__fill.fill-loader__fill--1st]]
     [:div.fill-loader__bar
      [:div.fill-loader__base]
      [:div.fill-loader__fill.fill-loader__fill--2nd]]
     [:div.fill-loader__bar
      [:div.fill-loader__base]
      [:div.fill-loader__fill.fill-loader__fill--3rd]]]]])

(defn loader
  []
  [:div.fill-loader.fill-loader--v5
   {:role "alert"}
   [:p.fill-loader__label "Content is loading..."]
   [:div
    {:aria-hidden "true"}
    [:div.fill-loader__base]
    [:div.fill-loader__fill.fill-loader__fill--1st]
    [:div.fill-loader__fill.fill-loader__fill--2nd]
    [:div.fill-loader__fill.fill-loader__fill--3rd]
    [:div.fill-loader__fill.fill-loader__fill--4th]]])

(defn find-by-id
  "Given a list of maps, find the first map where the :id key equals the given id"
  [data id]
  (medley/find-first #(= id (:id %)) data))

(defn format-tx-time
  "Given an instant, returns a human readable 12 hour time string. E.g '02:00PM'"
  [tx-time]
  #?(:cljs (some-> tx-time
                   tick/inst
                   (.toLocaleTimeString
                    #js []
                    #js {:hour12 true
                         :hour "numeric"
                         :minute "numeric"}))
     ;; there's probably a better way to do this but at least its easy to
     ;; understand and consistant with the JS version
     :clj (let [minute (tick/minute tx-time)
                hour (tick/hour tx-time)]
            (cond
              (= (tick/noon) tx-time)
              "12:00 NOON"

              (>= hour 13)
              (format "%02d:%02d PM" (- hour 12) minute)

              (>= hour 12)
              (format "%02d:%02d PM" hour minute)

              (< hour 12)
              (format "%02d:%02d AM" hour minute)))))


(defn exception? [e]
  (instance? #?(:clj Exception :cljs js/Error) e))

(s/fdef log-throw
  :args (s/alt :one (s/cat :e exception?)
               :two (s/cat :e exception?
                           :msg string?)
               :three (s/cat :e exception?
                             :msg string?
                             :extra-log-info (s/nilable map?))))

(defn log-throw
  "Takes an exception and an optional error message and an optional extra info
  map. Logs the ex-message and ex-data of the exception at the error level by
  default. Instead logs `msg` if provided. Merges `extra-log-info` over the
  ex-data if provided. Finally, throws exception `e`."
  ([e]
   (log-throw e (ex-message e)))
  ([e msg]
   (log-throw e msg nil)
   (throw e))
  ([e msg extra-log-info]
   (let [data (ex-data e)]
     (log/error e msg (-> (if (= :thanks.api.errors/exception (:type data))
                            (:info data)
                            data)
                          (merge extra-log-info))))
   (throw e)))
