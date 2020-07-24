(ns PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.components :as components]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.subscriptions :as sub]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.http :as http]))

(defn views
  []
  ;;TODO this belongs in the route structure
  (rf/dispatch [::http/fetch-data])
  (let [modal-state (r/atom nil)]
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
            [:p.font-italic
             "User customisable dashboard with custom charts, alerts and summary tables."])]
         [components/modal]]))))
