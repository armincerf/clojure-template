(ns PROJECTNAMESPACE.PROJECTNAME.frontend.navigation
  (:require
   [re-frame.core :as rf]
   [reitit.frontend :as reitit]
   [reitit.coercion.spec :as rss]
   [reitit.frontend.controllers :as rfc]
   [reitit.frontend.easy :as rfe]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.routes :as routes]))

(rf/reg-fx
 ::navigate!
 (fn [route]
   (apply rfe/push-state route)))

(rf/reg-event-fx
 :navigate
 (fn [_ [_ & route]]
   {::navigate! route}))

(rf/reg-event-fx
 ::navigated
 (fn [{:keys [db]} [_ new-match]]
   (let [old-match (:current-route db)
         controllers (rfc/apply-controllers (:controllers old-match) new-match)]
     (merge
      {:db (assoc db
                  :current-route
                  (assoc new-match :controllers controllers)
                  :show-menu? false)}
      (when-let [evts (:dispatch-n (:data new-match))]
        {:dispatch-n evts})
      (when-let [evt (:dispatch (:data new-match))]
        {:dispatch evt})))))

(defn on-navigate [new-match]
  (when new-match
    (rf/dispatch [::navigated new-match])))

(def router
  (reitit/router routes/routes {:data {:coercion rss/coercion}}))

(defn init-routes! []
  (js/console.log " routes")
  (rfe/start!
   router
   on-navigate
   {:use-fragment false}))
