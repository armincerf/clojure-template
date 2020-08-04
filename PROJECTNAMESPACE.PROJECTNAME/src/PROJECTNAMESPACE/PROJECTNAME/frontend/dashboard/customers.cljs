(ns PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.customers
  (:require [PROJECTNAMESPACE.PROJECTNAME.frontend.uikit.table :as table]
            [PROJECTNAMESPACE.PROJECTNAME.common :as common]
            [PROJECTNAMESPACE.PROJECTNAME.frontend.common :as frontend.common]
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
  (let [customer @(rf/subscribe [::sub/customer-profile])
        loading? @(rf/subscribe [:loading?])
        editable-fields (dissoc customer :id)]
    (cond
      customer
      [:section.profile
       [:h1 (:customer/name customer) "'s Profile"]
       (when (seq customer)
         [frontend.common/auto-form editable-fields {:collection-name "customers"}])]
      loading?
      [common/loading-component "Loading Profile"]
      :else
      [:p "No customer found with that ID"])))

