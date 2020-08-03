(ns PROJECTNAMESPACE.PROJECTNAME.frontend.app.components
  (:require
   [PROJECTNAMESPACE.PROJECTNAME.frontend.routes :as routes]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.common :as common]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.app.subscriptions :as sub]
   [clojure.string :as str]
   [clojure.pprint :as pprint]
   [reagent.core :as r]
   [re-frame.core :as rf]))

(defn modal-container
  [{:keys [child title state]}]
  [:div.modal-wrapper
   [:div.modal-backdrop
    {:on-click (fn [event]
                 (do
                   (rf/dispatch [:modal {:show? false
                                         :child nil}])
                   (.preventDefault event)
                   (.stopPropagation event)))}]
   [:div.modal-child
    [:div.modal__box-header
     {:class (case state
               :error "modal__box-header--error"
               "")}
     [:h4.modal__box-header-title title]
     [:i.modal__box-header-icon.fas.fa-times
      {:on-click (fn [event]
                   (do
                     (rf/dispatch [:modal {:show? false
                                           :child nil}])
                     (.preventDefault event)
                     (.stopPropagation event)))}]]
    [:div.modal__box-body
     [common/pprint-code {:key "value"
                          :nested {:foo "bar"
                                   :nested {:foo "bar"
                                            :nested {:foo "bar"}}}}]]
    [:div.modal__box-footer]]])

(defn modal
  []
  (let [modal (rf/subscribe [:modal])]
    (fn []
      (when (:show? @modal)
        [modal-container @modal]))))

(defn breadcrumb
  "data is a vector of maps i.e. {:label 'bar' :href 'foo'}"
  [data]
  [:div.breadcrumb
   (for [{:keys [label href]} (drop-last data)]
     ^{:key label}
     [:<>
      [:a.breadcrumb__link
       {:href href}
       label]
      [:span.breadcrumb__divider " / "]])
   [:a.breadcrumb__link.breadcrumb--active
    {:href (:href (last data))}
    (:label (last data))]])

(defn logo
  [sidebar?]
  [:div.logo
   [:img.logo-img
    {:src "/img/logo.svg"
     :class (when-not sidebar? "logo-img--mobile")}]
   (when sidebar?
     [:img.logo-img--mobile
      {:src "/img/logo-big.svg"}])])

(defn header
  [current-page]
  [:div.header.bg-black
   [:div.header__container
    [:a.mobile-logo
     {:on-click #(rf/dispatch [:menu-toggle])}
     [logo false]]
    [:div.header__nav
     [breadcrumb [{:label "Breadcrumb1"}
                  {:label "Breadcrumb2"}
                  {:label "Breadcrumb Active"}]]]
    [:div.header__icons
     [:a
      [:i.fas.fa-search]]
     [:a
      [:i.fas.fa-bell]]
     [:a.header--mobile
      [:i.fas.fa-bars
       {:on-click #(rf/dispatch [:menu-toggle])}]]]]])

(defn select
  [component & [args]]
  (let [!ref-button (atom nil)
        !ref-items (atom nil)
        active? (r/atom false)
        handler (fn [e]
                  (let [^js node (.-target e)]
                    (if (.contains @!ref-button node)
                      (swap! active? not)
                      (reset! active? false))))
        ref-button (fn [el] (reset! !ref-button el))
        ref-items (fn [el] (reset! !ref-items el))]
    (r/create-class
     {:component-did-mount
      (fn []
        (js/document.addEventListener "mouseup" handler))
      :component-will-unmount
      (fn []
        (js/document.removeEventListener "mouseup" handler))
      :reagent-render
      (fn [component]
        [component {:active? @active?
                    :ref-button ref-button
                    :ref-items ref-items
                    :args args}])})))

(defn nav
  [current-page]
  (let [show-menu? @(rf/subscribe [:show-menu?])]
    [:div.nav
     {:class (when show-menu? "open")}
     [logo true]
     [:div.menu
      [:ul
       (for [{:keys [link-text name disabled? icon children]}
             (filter :icon routes/dashboard-pages)]
         ^{:key link-text}
         [:li
          {:class (when (or (= current-page name)
                            ((set children) current-page)) "active")}
          [:a {:href (when name (common/route->url name))}
           [:i.menu__icon.fas.fa-fw
            {:class icon}]
           [:span.text link-text]]])]]
     [:div.settings.menu__settings
      [:a
       {:href (common/route->url :dashboard/settings)
        :class (when (= current-page :settings) "active")}
       [:i.menu__icon.fas.fa-fw.fa-cog]]]
     [:div.close-nav
      {:on-click #(rf/dispatch [:menu-toggle])}
      "x"]]))

(defn page-title
  ([props]
   (page-title props nil))
  ([{:keys [title]} action]
   [:div
    [:div.page__title-container
     [:h1.page__title title]
     (when action action)]
    [:hr.page__title-hr]]))

(defn page-subtitle
  ([props]
   (page-subtitle props nil))
  ([{:keys [subtitle description]} action]
   [:div.page__subtitle-container
    [:div
     [:h2.page__subtitle subtitle]
     (when description
       [:div.page__subtitle-description description])]
    (when action action)]))

(defn page-menu-item
  [{:keys [path link-text nav-icon]} li-props]
  [:a {:href path}
   [:div.sidenav__item
    [:img {:src (str "/img/" nav-icon)}]
    [:li li-props link-text]]])
