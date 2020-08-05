(ns PROJECTNAMESPACE.PROJECTNAME.frontend.app.subscriptions
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :db
 (fn [db _] db))

(rf/reg-sub
 :debug?
 (fn [db _] (:debug? db)))

(rf/reg-sub
 :modal
 (fn [db _] (:modal db)))

(rf/reg-sub
 :show-menu?
 (fn [db _] (:show-menu? db)))

(rf/reg-sub
 ::current-route
 (fn [db _]
   (:current-route db)))

(rf/reg-sub
 ::page
 :<- [::current-route]
 (fn [current-route _]
   (get-in current-route [:data :name])))

(rf/reg-sub
 ::page-title
 :<- [::current-route]
 (fn [current-route _]
   (get-in current-route [:data :link-text])))

(rf/reg-sub
 ::path-params
 (fn [_ _]
   (rf/subscribe [::current-route]))
 (fn [current-route _]
   (get current-route :path-params)))

(rf/reg-sub
 ::query-params
 (fn [_ _]
   (rf/subscribe [::current-route]))
 (fn [current-route _]
   (get current-route :query-params)))

(rf/reg-sub
 ::user
 (fn [db _]
   (:user db)))

(rf/reg-sub
 ::current-asset
 (fn [db _]
   (get-in db [:data :current-asset])))

(rf/reg-sub
 ::assets
 (fn [db _]
   (get-in db [:data :assets])))

