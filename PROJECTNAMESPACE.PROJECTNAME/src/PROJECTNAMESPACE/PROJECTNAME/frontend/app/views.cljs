(ns PROJECTNAMESPACE.PROJECTNAME.frontend.app.views
  (:require
   [re-frame.core :as rf]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.app.components :as components]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.app.subscriptions :as sub]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.global-messages :as messages]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.http :as http]))

(defn home
  []
  [:div "HI"])

(defn views
  []
  (fn []
    (let [page @(rf/subscribe [::sub/page])]
      [:<>
       [components/header page]
       [components/nav page]
       [:div.page-content
        (case page
          :app/homepage [home]
          [:p.font-italic
           (str "No content for page " page)])]
       [components/modal]
       [messages/toast]])))
