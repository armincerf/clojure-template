(ns PROJECTNAMESPACE.PROJECTNAME.frontend.alerts
  (:require
   [PROJECTNAMESPACE.PROJECTNAME.frontend.uikit.table :as table]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.subscriptions :as sub]
   [re-frame.core :as rf]))

(defn alerts
  []
  (let [table-data @(rf/subscribe [::sub/alerts-table])]
    [:<>
     [:p "Alerts"]
     [table/table table-data]]))
