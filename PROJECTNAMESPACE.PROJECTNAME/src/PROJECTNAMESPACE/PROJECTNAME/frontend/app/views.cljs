(ns PROJECTNAMESPACE.PROJECTNAME.frontend.app.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [fork.re-frame :as fork]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.common :as common]
   [PROJECTNAMESPACE.PROJECTNAME.common :as common-shared]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.app.components :as components]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.app.subscriptions :as sub]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.global-messages :as messages]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.http :as http]
   [clojure.string :as str]))

(defn type->icon-class
  [type]
  (str "asset-icon " (common-shared/keyword->string type) "-icon"))

(defn type-selector
  [selected-type]
  (let [type-keyword @selected-type]
    [:div.asset-types
     (for [asset-type [:email :phone-number :password]
           :let [active? (= type-keyword asset-type)]]
       ^{:key asset-type}
       [:a.card.asset-type
        {:class (when active? "asset-type--active")
         :on-click #(reset! selected-type asset-type)}
        (common-shared/keyword->readable-string asset-type)])]))

(defn data-input
  [{:keys [handle-change handle-blur submitting?]} options]
  [:input.form-control.w-input
   (merge
    {:disabled submitting?
     :on-change handle-change
     :on-blur handle-blur}
    options)])

(defn text-input
  [{:keys [values] :as props} key]
  [data-input props
   {:name key
    :type "text"
    :value (values key)}])

(defn form-input
  [label input]
  [:div.margin-bottom-sm
   [:label.form-label.form__label
    label]
   [:div.form-control-wrapper input]])

(defn asset-profile
  []
  (let [{:keys [id
                asset/name
                asset/description
                asset/data
                asset/type
                asset/breaches] :as asset}
        @(rf/subscribe [::sub/current-asset])
        type-str (common-shared/keyword->readable-string type)
        last-scan (:tx-time breaches)
        breaches (:breach/data breaches)]
    [:div {:class "page-content w-clearfix"}
     [:h1 {:class "page-title"}
      [components/breadcrumb
       [{:label "Assets"
         :href (common/route->url :app/homepage)}
        {:label name}]]]
     [:div {:class "topdashbar"}
      [:div {:class "stathold"}
       [:div {:class "statrow w-row"}
        [:div {:class "leftcolstat w-col w-col-4 w-col-stack"}
         [:div {:class "circlewicon nomeetingsicon"}]]
        [:div {:class "column-7 w-col w-col-8 w-col-stack"}
         [:div {:class "topdashbartext"} "Risk score"]
         [:div {:class "topdashbartext number"} "36"]
         [:div {:class "topdashbartext number desctext"} "/100"]]]]
      [:div {:class "stathold"}
       [:div {:class "statrow w-row"}
        [:div {:class "w-col w-col-4 w-col-stack"}
         [:div {:class "circlewicon rescheduleicon"}]]
        [:div {:class "column-8 w-col w-col-8 w-col-stack"}
         [:div {:class "topdashbartext"} name]
         [:div {:class "topdashbartext number"} "2"]
         [:div {:class "topdashbartext number desctext"} "This Month"]]]]
      [:div {:class "stathold laststat bottomlaststat"}
       [:div {:class "statrow statlastbottom w-row"}
        [:div {:class "w-col w-col-4 w-col-stack"}
         [:div {:class "circlewicon cancelledmeetings"}]]
        [:div {:class "column-9 w-col w-col-8 w-col-stack"}
         [:div {:class "topdashbartext"} "Total scans"]
         [:div {:class "topdashbartext number"} "5"]
         [:div {:class "topdashbartext number desctext"} "This Month"]]]]]
     [:div.asset-details.card
      [:div.asset-details__icon.circlewicon
       {:class (type->icon-class type)}]
      [:h3 "Asset Details"]
      [:div.asset-details__label
       name]
      [:div.asset__data data]
      (when description
        [:div.asset-details__item
         [:div.asset-details__label "Description:"]
         [:div.asset__description description]])
      [:div.asset-details__item
       [:div.asset-details__label "Last scan:"]
       [:div.asset__description (common-shared/format-tx-time last-scan)]]
      [:h3 "Breaches"]
      [:div.asset-details__breaches
       (if (seq breaches)
         [:div.breaches
          [:p "Oh jeez... looks like the following websites/companies have been
         hacked and leaked your private information! Click on a name to show
         more details about the breach."]
          (for [{:keys [Name]} breaches]
            ^{:key Name}
            [:a Name])]
         [:p "Good news! Your data has not been found in any data breaches or
         hacks"])
       [:button
        {:on-click #(rf/dispatch [:asset/search-breaches id])}
        "Search for new breaches"]]]]))

(defn add-data
  []
  (let [selected-type (r/atom :email)]
    (fn []
      [:div {:class "page-content w-clearfix"}
       [:h1.page-title "Add data"]
       [type-selector selected-type]
       [fork/form {:path :new-asset
                   :form-id "asset"
                   :prevent-default? true
                   :clean-on-unmount? true
                   :on-submit #(rf/dispatch [:asset/create @selected-type %])
                   :initial-values
                   {:asset/name ""
                    :asset/description ""
                    :asset/data ""}}
        (fn [{:keys [values
                     form-id
                     handle-submit
                     submitting?
                     reset] :as props}]
          [:div.form
           [:form.profile-component__form
            {:id form-id
             :on-submit handle-submit}
            [form-input "Name" [text-input props "asset/name"]]
            [form-input "Description" [text-input props "asset/description"]]
            [form-input
             (common-shared/keyword->readable-string @selected-type)
             [data-input props
              {:type (if (= :password @selected-type) "password" "text")
               :name "asset/data"
               :value (values "asset/data")}] ]
            [:button.btn.btn--md.form__submit
             {:type "submit"
              :disabled submitting?}
             "Submit"]]])]])))

(defn account-settings
  []
  [:div "settings"])

(defn my-assets
  []
  (let [assets @(rf/subscribe [::sub/assets])]
    [:div.assets
     (for [{:keys [id
                   asset/data
                   asset/name
                   asset/type]} assets
           :let [last-scan "Last scanned - 10:30 AM"]]
       ^{:key id}
       [:div {:class "meetingdetailcontain float-left"}
        [:div {:class "meetingdetailtopdiv"}
         [:div {:class "w-row"}
          [:div {:class "column-10 w-col w-col-5"}
           [:div {:class (type->icon-class type)}]]
          [:div {:class "w-col w-col-7"}
           [:div {:class "meetingtitle"}
            (common-shared/keyword->readable-string type)]
           [:div {:class "meetingtitle meetingemail"}
            (if (= :password type)
              "*********"
              data)]
           [:div {:class "meetingtitle meetingemail meetingtime w-hidden-small w-hidden-tiny"} last-scan]]]]
        [:div {:class "meetingdetailmiddlediv"}
         [:div {:class "w-row"}
          [:div {:class "w-col w-col-6"}
           [:div {:class "alert-text"} (rand-nth ["No" "2" "3"]) " New Alerts"]]
          [:div {:class "w-col w-col-6"}
           [:div {:class "alert-text resolved-alert"} (rand-nth ["1" "3"]) " Resolved Alerts"]]]]
        [:div {:class "meetingtitle meetingemail only-mobile"}
         last-scan]
        [:a {:href (common/id-route :app/asset-profile {:asset id})
             :class "bottommorelink bottommoreright w-inline-block"}
         [:div {:class "detailscallinktext"} "View Details"]]])]))

(defn overview
  []
  [:div {:class "page-content w-clearfix"}
   [:h1 {:class "page-title"} "Assets"]
   [:div {:class "topdashbar"}
    [:div {:class "stathold"}
     [:div {:class "statrow w-row"}
      [:div {:class "leftcolstat w-col w-col-4 w-col-stack"}
       [:div {:class "circlewicon nomeetingsicon"}]]
      [:div {:class "column-7 w-col w-col-8 w-col-stack"}
       [:div {:class "topdashbartext"} "Data Protected"]
       [:div {:class "topdashbartext number"} "36"]
       [:div {:class "topdashbartext number desctext"} "Items"]]]]
    [:div {:class "stathold"}
     [:div {:class "statrow w-row"}
      [:div {:class "w-col w-col-4 w-col-stack"}
       [:div {:class "circlewicon rescheduleicon"}]]
      [:div {:class "column-8 w-col w-col-8 w-col-stack"}
       [:div {:class "topdashbartext"} "Alerts"]
       [:div {:class "topdashbartext number"} "14"]
       [:div {:class "topdashbartext number desctext"} "This Month"]]]]
    [:div {:class "stathold laststat bottomlaststat"}
     [:div {:class "statrow statlastbottom w-row"}
      [:div {:class "w-col w-col-4 w-col-stack"}
       [:div {:class "circlewicon cancelledmeetings"}]]
      [:div {:class "column-9 w-col w-col-8 w-col-stack"}
       [:div {:class "topdashbartext"} "Total scans"]
       [:div {:class "topdashbartext number"} "20"]
       [:div {:class "topdashbartext number desctext"} "This Month"]]]]]
   [:div {:class "numberofmeetings"}
    "My Assets"
    [my-assets]]])

(defn home
  []
  )

(defn inbox
  [show-inbox?]
  (fn []
    [:div {:class "inboxcontain"
           :style {:display (if @show-inbox? "block" "none")}}
     [:div {:class "iteminboxdiv"}
      [:div {:class "iteminboxdetaildiv"}
       [:div {:class "div-block-8"}]
       [:div {:class "iteminboxdetailtext"} "01/01/2020 10:30"]]
      [:div
       [:div "Email found in Yahoo data leak!"]
       [:div {:class "inboxnumbertext"} "work-email@fakemail.com"]]]
     [:div {:class "iteminboxdiv"}
      [:div {:class "iteminboxdetaildiv"}
       [:div {:class "div-block-8"}]
       [:div {:class "iteminboxdetailtext"} "01/05/2020 12:30"]]
      [:div
       [:div "Email found in Hotmail data leak!"]
       [:div {:class "inboxnumbertext"} "home-email@fakemail.com"]]]
     [:div {:class "iteminboxdiv itembuttondiv"}
      [:a {:class "inboxbutton leftinboxbutton w-inline-block"}
       [:div "Refresh"]]
      [:a {:data-ix "new-interaction-3"
           :class "inboxbutton w-inline-block"
           :on-click #(do
                        (.preventDefault %)
                        (reset! show-inbox? false))}
       [:div "Close"]]]
     (when-not show-inbox?
       [:div {:class "iteminboxdiv bottombelldiv w-clearfix"}
        [:div {:class "meetingrightdiv"}]
        [:div {:class "w-row"}
         [:div {:class "w-col w-col-4"}
          [:div {:class "calimginbox bottombellimg"}]]
         [:div {:class "w-col w-col-8"}
          [:div "Inbox"]
          [:div {:class "inboxdate w-embed w-script"}]]]])]))

(defn views
  []
  (let [page @(rf/subscribe [::sub/page])
        show-inbox? (r/atom false)]
    [:<>
     [components/top-nav]
     [:div {:class "section"}
      [:a {:data-ix "new-interaction-2"
           :class "inboxdiv w-hidden-small w-hidden-tiny w-inline-block"
           :on-click #(do
                        (.preventDefault %)
                        (swap! show-inbox? not))}
       [:div {:class "bell"} "H"]]
      [:div {:class "pagecontain"}
       [:div {:class "div-block-6"}
        [components/left-menu page]
        (case page
          :app/homepage [overview]
          :app/add-data [add-data]
          :app/asset-profile [asset-profile]
          :app/account-settings [account-settings]
          [:p.font-italic
           (str "No content for page " page)])]]
      [inbox show-inbox?]]
     [components/modal]
     [messages/toast]]))
