(ns PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.customers
  (:require [PROJECTNAMESPACE.PROJECTNAME.frontend.uikit.table :as table]
            [PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.subscriptions :as sub]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [PROJECTNAMESPACE.PROJECTNAME.frontend.common :as common]))

(defn page
  []
  (let [checkbox (r/atom nil)]
    (fn []
      (let [page-title @(rf/subscribe [::sub/page-title])]
        [:<>
         [:h1.text-lg
          page-title]
         [:div.component-card
          [table/table @(rf/subscribe [::sub/customers-table])]]]))))
