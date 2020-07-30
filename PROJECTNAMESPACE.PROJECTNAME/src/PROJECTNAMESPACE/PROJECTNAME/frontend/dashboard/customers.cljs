(ns PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.customers
  (:require [PROJECTNAMESPACE.PROJECTNAME.frontend.uikit.table :as table]
            [PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.subscriptions :as sub]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.components :as components]))

(defn page
  []
  (let [page-title @(rf/subscribe [::sub/page-title])
        table-data @(rf/subscribe [::sub/customers-table])]
    [:<>
     [:h1.text-lg
      page-title]
     [:div.component-card
      [table/table table-data]]]))

(defn profile
  []
  (let [checkbox (r/atom nil)]
    (fn []
      (let [page-title @(rf/subscribe [::sub/page-title])
            customer @(rf/subscribe [::sub/customer-profile])]
        [:<>
         [:h1.text-lg
          (components/pprint-code customer)]]))))
