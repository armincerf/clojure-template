(ns PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.events
  (:require [clojure.set :as set]
            [clojure.string :as string]
            [re-frame.core :as rf]
            [PROJECTNAMESPACE.PROJECTNAME.frontend.ajax :as ajax]
            [medley.core :as medley]
            [clojure.string :as str]))

(rf/reg-event-fx
 :dashboard/data-fetch-success
 (fn [{:keys [db]} [_ key response]]
   {:db (-> db
            (assoc-in [:data key] (:data response))
            (assoc :loading? false))}))

(rf/reg-event-fx
 :dashboard/data-fetch
 (fn [{:keys [db]} [_ url]]
   (let [key (subs url (inc (str/last-index-of url "/")))]
     (assoc
      (ajax/get-request url
                        [:dashboard/data-fetch-success (keyword "dashboard" key)]
                        [:generic-failure])
      :db (assoc db :loading? true)))))

(rf/reg-event-fx
 :alert-notification/add
 (fn [{:keys [db]} [_ alert]]
   {:dispatch [:global-message/add {:global-message/type :message/error
                                    :content
                                    {:header "Alert Notification"
                                     :body [:div.notification__body
                                            [:i.fa.fa-exclamation-circle.notification__icon]
                                            [:div.notification-body__text
                                             [:p.boldunderline (:asset/name alert)]
                                             [:p (:company-name alert)]]]}}]}))

(rf/reg-event-fx
 :data/update
 (fn [{:keys [db]} [_ {:keys [values]} collection-name]]
   (let [data-url (str "/api/v1/" collection-name)]
     (assoc
      (ajax/put-request (str data-url "/" (first (vals (get-in db [:current-route :path-params]))))
                        (medley/map-keys keyword values)
                        [:dashboard/data-fetch data-url]
                        [:generic-failure])
      :db (assoc db :loading? true)))))
