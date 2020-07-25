(ns PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.events
  (:require [clojure.set :as set]
            [clojure.string :as string]
            [re-frame.core :as rf]
            [PROJECTNAMESPACE.PROJECTNAME.frontend.global-messages :as message]
            [PROJECTNAMESPACE.PROJECTNAME.frontend.ajax :as ajax]
            [medley.core :as medley]))

(rf/reg-event-fx
 :dashboard/data-fetch-success
 (fn [{:keys [db]} [_ response]]
   {:db (-> db
            (assoc-in [:data (:key response)]  (:data response))
            (assoc-in [:page-state ::product-fetch-failure] nil)
            (assoc ::loading? false))}))

(rf/reg-event-fx
 :dashboard/data-fetch-failure
 (fn [{:keys [db]} [_ response]]
   (js/console.error "fetch failed" response)
   {:dispatch [:global-message/add (message/error response)]
    :db (-> db (assoc-in [:page-state :dashboard/product-fetch-failure]
                         "Something went wrong fetching the data"))}))

(rf/reg-event-fx
 :dashboard/customers-fetch
 (fn [{:keys [db]} _]
   (assoc
    (ajax/get-request "/api/v1/customers"
                      [:dashboard/data-fetch-success]
                      [:dashboard/data-fetch-failure])
    :db (assoc db ::loading? true))))

(rf/reg-event-fx
 :alert-notification/add
 (fn [{:keys [db]} [_ alert]]
   {:dispatch [:global-message/add {:global-message/type :message/error
                                    :content
                                    {:header "Alert Notification"
                                     :body [:div.notification__body
                                            [:i.fa.fa-exclamation-circle.notification__icon]
                                            [:div.notification-body__text
                                             [:p.boldunderline (:asset-name alert)]
                                             [:p (:company-name alert)]]]}}]}))

(rf/reg-event-fx
 :global-message/add
 (fn [{:keys [db]} [_ message]]
   {:db (assoc db :global-message message)
    :dispatch-later [{:ms 3000
                      :dispatch [:global-message/dismiss]}]}))

(rf/reg-event-db
 :global-message/dismiss
 (fn [db _]
   (dissoc db :global-message)))
