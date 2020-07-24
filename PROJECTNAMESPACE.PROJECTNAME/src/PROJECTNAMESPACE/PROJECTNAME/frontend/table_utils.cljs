(ns PROJECTNAMESPACE.PROJECTNAME.frontend.table-utils
  (:require [tick.alpha.api :as t]
            [tick.format :as tf]
            [tick.locale-en-us]))

(def locale js/navigator.language)

(defn date-component
  [_m v]
  (t/format (tf/formatter
             (if (= "en-US" locale)
               "MM-dd-YYYY HH:mm:ss"
               "dd-MM-YYYY HH:mm:ss"))
            (t/date-time (t/instant v))))

(defn link-component
  [href v]
  [:a.table__link
   {:href href}
   v])

(defn icon-component
  [m v]
  (let [icon (case v
               "overload" "fa-exclamation-circle"
               "fa-stop-circle")]
    [:span.table__alert-group
     {:class (case v
               "overload" "table__alert-group--red"
               "table__alert-group--red")}
     [:i.fas.table__alert-icon
      {:class icon}]
     v]))
