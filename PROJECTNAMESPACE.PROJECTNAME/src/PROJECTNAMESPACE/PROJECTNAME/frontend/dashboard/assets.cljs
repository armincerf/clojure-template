(ns PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.assets
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
        table-data @(rf/subscribe [::sub/assets-table])]
    [:<>
     [:h1.text-lg
      page-title]
     [:div.component-card
      [table/table table-data]]]))

(defn type-select-box
  [{:keys [values handle-change handle-blur reset]}]
  [components/select
   (fn [{:keys [active? ref-button ref-items]}]
     [:<>
      [:div
       [:button.btn.btn--subtle
        {:ref ref-button
         :on-click #(.preventDefault %)}
        (get values "asset/type")
        [:i.fas.fa-caret-down.margin-left-xxs]]]
      [:div.select
       {:ref ref-items
        :class (if active?
                 "select--show"
                 "select--hide")}
       (for [{:keys [label value]} [{:label "email" :value :email}
                                    {:label "password" :value :password}
                                    {:label "phone" :value :phone}]]
         ^{:key (str label value)}
         [:option.option
          {:name "asset/type"
           :value value
           :on-click handle-change
           :on-blur handle-blur}
          label])]])])

(defn customer-select-box
  [{:keys [values handle-change handle-blur reset]}]
  (let [selected-customer (r/atom nil)
        item-key "asset/customer"
        customers @(rf/subscribe [:dashboard/data :dashboard/customers])]
    (fn []
      [components/select
       (fn [{:keys [active? ref-button ref-items]}]
         [:<>
          [:div
           [:button.btn.btn--subtle
            {:ref ref-button
             :on-click #(.preventDefault %)}
            (or @selected-customer
                (:customer/email
                 @(rf/subscribe [::sub/customer-profile
                                 (get values item-key)])))
            [:i.fas.fa-caret-down.margin-left-xxs]]]
          [:div.select
           {:ref ref-items
            :class (if active?
                     "select--show"
                     "select--hide")}
           (let [data (for [customer customers]
                        {:label (:customer/email customer)
                         :value (common/keyword->string (:id customer))})]
             (for [{:keys [label value]} data]
               ^{:key (str label value)}
               [:option.option
                {:name item-key
                 :value value
                 :on-click
                 #(do
                    (reset! selected-customer label)
                    ;; I need to do this because it seems like keywords are
                    ;; turned into strings when set as the value attr, so we
                    ;; must call keyword on them before putting them in the
                    ;; state
                    (fn handle-change
                      [state]
                      (swap! state update :values
                             assoc item-key (-> % .-target .-value keyword))))
                 :on-blur handle-blur}
                label]))]])])))

(defn profile
  []
  (let [asset @(rf/subscribe [::sub/asset-profile])
        loading? @(rf/subscribe [:loading?])
        editable-fields (dissoc asset :id)]
    (cond
      loading?
      [common/loading-component "Loading Profile"]
      asset
      [:section.profile
       [:h1 (:asset/name asset)]
       (when (seq asset)
         [frontend.common/auto-form editable-fields
          {:collection-name "assets"
           :custom-components
           {:asset/type type-select-box
            :asset/customer customer-select-box}}])]
      :else
      [:p "No asset found with that ID"])))
