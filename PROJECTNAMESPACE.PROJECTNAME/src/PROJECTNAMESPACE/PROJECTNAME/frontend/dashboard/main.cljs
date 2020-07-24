(ns ^:figwheel-hooks PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.main
  (:require [day8.re-frame.http-fx]
            [re-frame.core :as rf]
            [re-pressed.core :as rp]
            [reagent.dom :as r]
            PROJECTNAMESPACE.PROJECTNAME.frontend.dev
            PROJECTNAMESPACE.PROJECTNAME.frontend.events
            PROJECTNAMESPACE.PROJECTNAME.frontend.http
            [PROJECTNAMESPACE.PROJECTNAME.frontend.navigation :as nav]
            [PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.views :as views]))

(defn mount-root
  []
  (when-let [section (js/document.getElementById "app")]
    (rf/clear-subscription-cache!)
    (r/render [views/views] section)))

(defn ^:export init
  []
  (rf/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (rf/dispatch [:init])
  (nav/init-routes!)
  (mount-root))

(defn ^:after-load re-render []
  (mount-root))

(defonce run (init))
