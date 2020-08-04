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

(defn top-nav
  [current-page]
  (let [show-menu? @(rf/subscribe [:show-menu?])
        title "havemydatasbeenpwnedornot"]
    [:div {:class "top-nav"}
     [:div {:class "nav-top__container"}
      [:div {:class "w-hidden-medium w-hidden-small w-hidden-tiny w-row"}
       [:div {:class "top-nav__left w-col w-col-3"}
        [:div {:class "logo-text"} title]]
       [:div {:class "w-col w-col-6"}
        [:div {:class "w-form"}
         [:form {:id "email-form", :name "email-form", :data-name "Email Form", :class "form"}
          [:input {:type "text", :class "searchbar w-input", :max-length "256", :name "Search", :data-name "Search", :placeholder "Search", :id "Search"}]
          [:div {:data-delay "0", :data-hover "1", :class "w-dropdown"}
           [:div {:class "searchdrop w-dropdown-toggle"}
            [:div {:class "w-icon-dropdown-toggle"}]
            [:div "Category"]]
           [:nav {:class "droplistsearch w-dropdown-list"}
            [:a {:href "#", :class "ddcatlink w-dropdown-link"} "User"]
            [:a {:href "#", :class "ddcatlink ddbottom w-dropdown-link"} "Project"]]]]
         [:div {:class "successsearch w-form-done"}]
         [:div {:class "w-form-fail"}
          [:div "Oops! Something went wrong while submitting the form."]]]]
       [:div {:class "top-nav__right w-col w-col-3"}
        [:div {:class "top-nav__avatar"}]
        [:div {:class "div-block"}
         [:div {:class "user-name-text"} "Alex Davis"]
         [:div {:data-delay "0", :class "dropdown-2 w-dropdown"}
          [:div {:class "top-nav__plus w-dropdown-toggle"}
           [:div "+"]]
          [:nav {:class (str "profile-dropdown w-dropdown-list" (when show-menu? " w--open"))}
           [:a {:href "#", :data-ix "new-interaction-2", :class "leftmenulink leftlogout toplogout w-inline-block"}
            [:div {:class "w-row"}
             [:div {:class "w-hidden-main w-hidden-medium w-hidden-small w-hidden-tiny w-col w-col-2"}
              [:div {:class "intext"} "i"]]
             [:div {:class "w-col w-col-2"}
              [:div {:class "icontectleft iconbell"} "H"]]
             [:div {:class "w-col w-col-8"}
              [:div {:class "leftlinktext"} "Inbox"]]]]
           [:a {:href "#", :class "leftmenulink leftlogout toplogout w-inline-block"}
            [:div {:class "w-row"}
             [:div {:class "w-hidden-main w-hidden-medium w-hidden-small w-hidden-tiny w-col w-col-2"}
              [:div {:class "intext"} "i"]]
             [:div {:class "w-col w-col-2"}
              [:div {:class "icontectleft"} "D"]]
             [:div {:class "w-col w-col-8"}
              [:div {:class "leftlinktext"} "Profile"]]]]
           [:a {:href "#", :class "leftmenulink leftlogout toplogout w-inline-block"}
            [:div {:class "w-row"}
             [:div {:class "w-hidden-main w-hidden-medium w-hidden-small w-hidden-tiny w-col w-col-2"}
              [:div {:class "intext"} "i"]]
             [:div {:class "w-col w-col-2"}
              [:div {:class "icontectleft"} "F"]]
             [:div {:class "w-col w-col-8"}
              [:div {:class "leftlinktext"} "Log Out"]]]]]]]]]]
     [:div {:data-collapse "medium", :data-animation "default", :data-duration "400", :data-doc-height "1", :role "banner", :class "navbar w-hidden-main w-nav"}
      [:div {:class "row-3 w-row"}
       [:div {:class "column-24 w-hidden-tiny w-col w-col-5 w-col-small-6"}
        [:div {:class "user-name-text leftnametext mobilenavwelcome"} "Welcome, Alex!"]]
       [:div {:class "column-23 w-col w-col-7 w-col-small-6"}
        [:div {:class "top-nav__avatar"}]
        [:div {:class "div-block"}
         [:div {:class "user-name-text"} "Alex Davis"]]]]
      [:div {:class "w-container"}
       [:nav {:role "navigation", :class "nav-menu w-nav-menu"}
        [:a {:href "#", :class "leftmenulink w-inline-block"}
         [:div {:class "w-row"}
          [:div {:class "w-hidden-medium w-hidden-small w-hidden-tiny w-col w-col-2 w-col-small-4 w-col-tiny-4"}
           [:div {:class "intext"} "i"]]
          [:div {:class "w-col w-col-2 w-col-small-4 w-col-tiny-4"}
           [:div {:class "icontectleft iconbell"} "H"]]
          [:div {:class "w-col w-col-8 w-col-small-4 w-col-tiny-4"}
           [:div {:class "leftlinktext"} "Inbox"]]]]
        (for [{:keys [link-text name disabled? icon children]}
              (filter :icon routes/app-pages)]
          ^{:key name}
          [:a {:href (common/route->url name)
               :class
               (str "leftmenulink w-inline-block"
                    (when (= current-page name)
                      " currentlinkleft"))}
           [:div {:class "w-row"}
            [:div {:class "w-hidden-medium w-hidden-small w-hidden-tiny w-col w-col-2 w-col-small-4 w-col-tiny-4"}
             [:div {:class "intext"} "i"]]
            [:div {:class "w-col w-col-2 w-col-small-4 w-col-tiny-4"}
             [:div {:class "icontectleft"} icon]]
            [:div {:class "w-col w-col-8 w-col-small-4 w-col-tiny-4"}
             [:div {:class "leftlinktext"} link-text]]]])
        [:a {:href "#", :class "leftmenulink leftlogout w-inline-block"}
         [:div {:class "w-row"}
          [:div {:class "w-hidden-medium w-hidden-small w-hidden-tiny w-col w-col-2 w-col-small-4 w-col-tiny-4"}
           [:div {:class "intext"} "i"]]
          [:div {:class "w-col w-col-2 w-col-small-4 w-col-tiny-4"}
           [:div {:class "icontectleft"} "F"]]
          [:div {:class "w-col w-col-8 w-col-small-4 w-col-tiny-4"}
           [:div {:class "leftlinktext"} "Log Out"]]]]]
       [:div {:class "w-row"}
        [:div {:class "w-col w-col-3 w-col-small-3 w-col-tiny-3"}
         [:a {:href "#", :class "brand w-nav-brand"}
          [:div {:class "logo-text navbarlogotext"} title]]]
        [:div {:class "w-col w-col-6 w-col-small-6 w-col-tiny-6"}
         [:div {:class "w-form"}
          [:form {:id "email-form", :name "email-form", :data-name "Email Form", :class "form"}
           [:input {:type "text", :class "searchbar mobilesearch w-input", :max-length "256", :name "Search-2", :data-name "Search 2", :placeholder "Search", :id "Search-2"}]]
          [:div {:class "successsearch w-form-done"}]
          [:div {:class "w-form-fail"}
           [:div "Oops! Something went wrong while submitting the form."]]]]
        [:div {:class "w-col w-col-3 w-col-small-3 w-col-tiny-3"}
         [:div {:class "hamburger w-nav-button"}
          [:div {:class "w-icon-nav-menu"}]]]]]]]))

(defn left-menu
  [current-page]
  [:div {:class "menuleft flextleft"}
   [:div {:class "row w-row"}
    [:div {:class "w-col w-col-2"}
     [:div {:class "top-nav__avatar leftavatar"}]]
    [:div {:class "w-col w-col-10"}
     [:div {:class "user-name-text leftnametext"} "Welcome, Alex!"]
     [:div {:class "user-name-text leftnametext detailtext"} "Admin"]]]
   [:div {:class "leftmenucontain"}
    (for [{:keys [link-text name disabled? icon children]}
          (filter :icon routes/app-pages)]
      ^{:key name}
      [:a {:href (common/route->url name)
           :class
           (str "leftmenulink w-inline-block"
                (when (= current-page name)
                  " currentlinkleft"))}
       [:div {:class "w-row"}
        [:div {:class "w-col w-col-2"}
         [:div {:class "intext"} "i"]]
        [:div {:class "w-col w-col-2"}
         [:div {:class "icontectleft"} icon]]
        [:div {:class "w-col w-col-8"}
         [:div {:class "leftlinktext"} link-text]]]])
    [:a {:href "#", :class "leftmenulink leftlogout w-inline-block"}
     [:div {:class "w-row"}
      [:div {:class "w-col w-col-2"}
       [:div {:class "intext"} "i"]]
      [:div {:class "w-col w-col-2"}
       [:div {:class "icontectleft"} "F"]]
      [:div {:class "w-col w-col-8"}
       [:div {:class "leftlinktext"} "Log Out"]]]]]])

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
