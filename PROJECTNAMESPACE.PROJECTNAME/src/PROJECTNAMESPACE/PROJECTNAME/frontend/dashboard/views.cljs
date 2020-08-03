(ns PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.views
  (:require
   [re-frame.core :as rf]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.global-messages :as messages]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.homepage :as homepage]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.components :as components]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.assets :as assets]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.customers :as customers]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.settings :as settings]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.subscriptions :as sub]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.http :as http]))

(defn views
  []
  (fn []
    (let [page @(rf/subscribe [::sub/page])]
      [:<>
       [components/header page]
       [components/nav page]
       [:div.page-content
        [:div.breadcrumb--mobile
         [components/breadcrumb [{:label "Breadcrumb1"}
                                 {:label "Breadcrumb2"}
                                 {:label "Breadcrumb Active"}]]]
        (case page
          :homepage [homepage/page]
          :assets [assets/page]
          :customers [customers/page]
          :customer [customers/profile]
          :settings [settings/page]
          [:p.font-italic
           (str "No content for page " page)])]
       [components/modal]
       [messages/toast]])))
