(ns PROJECTNAMESPACE.PROJECTNAME.frontend.app.events
  (:require [clojure.set :as set]
            [clojure.string :as string]
            [re-frame.core :as rf]
            [PROJECTNAMESPACE.PROJECTNAME.common :as common]
            [PROJECTNAMESPACE.PROJECTNAME.frontend.app.components :as components]
            [PROJECTNAMESPACE.PROJECTNAME.frontend.common :as frontend.common]
            [PROJECTNAMESPACE.PROJECTNAME.frontend.ajax :as ajax]
            [medley.core :as medley]
            [clojure.string :as str]))

(rf/reg-event-fx
 :asset/fetch-all
 (fn [{:keys [db]} _]
   (assoc (ajax/get-request
           "/api/v1/assets"
           [:asset/fetch-all-success]
           [:generic-failure])
          :db (assoc db :loading? true))))

(rf/reg-event-fx
 :asset/fetch-all-success
 (fn [{:keys [db]} [_ response]]
   (let [key (get-in response [:data :id])]
     {:db (-> db
              (assoc-in [:data :assets] (:data response))
              (assoc :loading? false))})))

(rf/reg-event-fx
 :asset/fetch-by-id
 (fn [{:keys [db]} _]
   (let [id (get-in db [:current-route :path-params :asset])]
     (ajax/get-request (str "/api/v1/assets/" id)
                       [:asset/fetch-by-id-success]
                       [:generic-failure]))))

(rf/reg-event-fx
 :asset/fetch-by-id-success
 (fn [{:keys [db]} [_ response]]
   (let [key (get-in response [:data :id])]
     {:db (-> db
              (assoc-in [:data :current-asset] (:data response))
              (assoc :loading? false))})))

(rf/reg-event-fx
 :asset/create-success
 (fn [{:keys [db]} [_ response]]
   (let [id (common/id-key (get-in response [:data :id]))]
     {:dispatch [:navigate :app/asset-profile {:asset id}]})))

(rf/reg-event-fx
 :asset/create
 (fn [{:keys [db]} [_ selected-type {:keys [values]}]]
   (assoc
    (ajax/post-request "/api/v1/assets"
                       {:asset (assoc
                                (medley/map-keys keyword values)
                                :asset/type selected-type)}
                       [:asset/create-success]
                       [:generic-failure])
    :db (assoc db :loading? true))))

(rf/reg-event-fx
 :asset/search-breaches
 (fn [{:keys [db]} [_ id]]
   (assoc (ajax/get-request
           (str "/api/v1/breaches/" (common/id-key id))
           [:asset/search-breaches-success]
           [:generic-failure])
          :db (assoc db :loading? true))))

(rf/reg-event-fx
 :asset/search-breaches-success
 (fn [{:keys [db]} [_ response]]
   (let [key (get-in response [:data :id])]
     {:db (-> db
              (assoc-in
               [:data :current-asset :asset/breaches]
               (:data response))
              (assoc :loading? false))})))


(rf/reg-event-fx
 :asset/show-breach-detail
 (fn [{:keys [db]} [_ breach-name]]
   (prn breach-name)
   (assoc (ajax/get-request
           (str "/api/v1/breaches/" breach-name "/details")
           [:asset/show-breach-success]
           [:generic-failure])
          :db (assoc db :loading? true))))

(rf/reg-event-fx
 :asset/show-breach-success
 (fn [{:keys [db]} [_ response]]
   (let [data response]
     {:dispatch [:modal {:show? true
                         :title "Breach Details"
                         :child [components/breach-details response]}]
      :db (assoc db :loading? false)})))
