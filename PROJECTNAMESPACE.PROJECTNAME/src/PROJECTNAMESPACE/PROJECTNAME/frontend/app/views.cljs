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
        (common-shared/keyword->readable-string asset-type)
        (when-not active?
          [:i.fa.fa-plus])])]))

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

(defn add-data
  []
  (let [selected-type (r/atom :email)]
    (fn []
      [:div {:class "div-block-7 w-clearfix"}
       [:h1.meetingsh1 "Add data"]
       [type-selector selected-type]
       [fork/form {:path :new-asset
                   :form-id "asset"
                   :prevent-default? true
                   :clean-on-unmount? true
                   :on-submit #(rf/dispatch [:add-asset selected-type %])
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

(defn overview
  []
  [:div {:class "div-block-7 w-clearfix"}
   [:h1 {:class "meetingsh1"} "Alerts"]
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
       [:div {:class "topdashbartext"} "Total scans completed"]
       [:div {:class "topdashbartext number"} "20"]
       [:div {:class "topdashbartext number desctext"} "This Month"]]]]]
   [:div {:class "numberofmeetings"} "My Data - If you see a warning sign,
     we've found your details somewhere they shouldn't be."]
   [:div {:data-ix "meetingdetail1", :class "meetingdetailcontain float-left"}
    [:div {:class "meetingdetailtopdiv"}
     [:div {:class "w-row"}
      [:div {:class "column-10 w-col w-col-5"}
       [:div {:class "meetingimgdiv"}]]
      [:div {:class "w-col w-col-7"}
       [:div {:class "meetingtitle"} "Email"]
       [:div {:class "meetingtitle meetingemail"} "adellac@josianne.com"]
       [:div {:class "meetingtitle meetingemail meetingtime w-hidden-small w-hidden-tiny"} "Last scanned - 10:30 AM"]]]]
    [:div {:class "meetingdetailmiddlediv"}
     [:div {:class "w-hidden-small w-hidden-tiny w-row"}
      [:div {:class "w-col w-col-6"}
       [:div {:class "alert-text"} "2 New Alerts"]]
      [:div {:class "w-col w-col-6"}
       [:div {:class "alert-text resolved-alert"} "3 Resolved Alerts"]]]
     [:div {:class "w-hidden-main w-hidden-medium w-row"}
      [:div {:class "column-25 w-col w-col-4 w-col-small-6 w-col-tiny-6"}
       [:div {:tooltipster "bottom-delay", :title "Lucas Davenport", :class "attendeediv attend1"}]
       [:div {:tooltipster "bottom-delay", :title "Shrake Jenkins", :class "attendeediv attend1 attendplu attend2"}]
       [:div {:title "Weather Karkinnen", :tooltipster "bottom-delay", :class "attendeediv attend1 attendplu attend3"}]
       [:div {:tooltipster "bottom-delay", :title "Eleanor Wish", :class "attendeediv attend1 attendplu attend4"}]
       [:div {:class "plusnumore w-hidden-main w-hidden-medium"} "+5 More"]]
      [:div {:class "column-26 w-col w-col-8 w-col-small-6 w-col-tiny-6"}
       [:div {:class "alert-text"} "9 Members Going"]
       [:div {:class "alert-text resolved-alert"} "2 Pending"]]]]
    [:a {:href "#", :class "bottommorelink w-hidden-main w-hidden-medium w-inline-block"}
     [:div {:class "detailscallinktext"} "10:30 AM"]]
    [:a {:href "#", :class "bottommorelink bottommoreright w-inline-block"}
     [:div {:class "detailscallinktext"} "View Details"]]]])

(defn home
  []
  )

(defn views
  []
  (let [show-inbox? (r/atom false)]
    (fn []
      (let [page @(rf/subscribe [::sub/page])]
        [:<>
         [components/top-nav]
         [:div {:class "section"}
          [:a {:href "#", :data-ix "new-interaction-2"
               :class "inboxdiv w-hidden-small w-hidden-tiny w-inline-block"
               :on-click #(swap! show-inbox? not)}
           [:div {:class "bell"} "H"]]
          [:div {:class "pagecontain"}
           [:div {:class "div-block-6"}
            [components/left-menu page]
            (case page
              :app/homepage [overview]
              :app/add-data [add-data]
              :app/account-settings [account-settings]
              [:p.font-italic
               (str "No content for page " page)])]]
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
            [:a {:href "#", :class "inboxbutton leftinboxbutton w-inline-block"}
             [:div "Refresh"]]
            [:a {:href "#", :data-ix "new-interaction-3", :class "inboxbutton w-inline-block"
                 :on-click #(reset! show-inbox? false)}
             [:div "Close"]]]
           (when-not show-inbox?
             [:div {:class "iteminboxdiv bottombelldiv w-clearfix"}
              [:div {:class "meetingrightdiv"}]
              [:div {:class "w-row"}
               [:div {:class "w-col w-col-4"}
                [:div {:class "calimginbox bottombellimg"}]]
               [:div {:class "w-col w-col-8"}
                [:div "Inbox"]
                [:div {:class "inboxdate w-embed w-script"}
                 [:script "var today = new Date();\nvar dd = today.getDate();\nvar mm = today.getMonth()+1; //January is 0!\nvar yyyy = today.getFullYear();\nif(dd<10) {\n    dd = '0'+dd\n} \nif(mm<10) {\n    mm = '0'+mm\n} \ntoday = mm + '/' + dd + '/' + yyyy;\ndocument.write(today);"]]]]])]]
         [components/modal]
         [messages/toast]]))))
