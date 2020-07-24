(ns PROJECTNAMESPACE.PROJECTNAME.frontend.http
  (:require [PROJECTNAMESPACE.PROJECTNAME.frontend.ajax :as ajax]
            [clojure.walk :as walk]
            [re-frame.core :as rf]))

(rf/reg-event-fx
 :generic-failure
 (fn [{:keys [db]} [_ result]]
   (js/console.error "Failed to load something" (:response result))
   {:db (-> db
            (assoc :loading? false)
            (assoc :errors (:response result)))}))

(rf/reg-event-fx
 ::me
 (fn [_ _]
   (ajax/get-request "/api/me"
                     [:me/success]
                     [:generic-failure])))

(rf/reg-event-db
 :me/success
 (fn [db [_ result]]
   (assoc db :user result)))

(rf/reg-event-fx
 ::fetch-data
 (fn [_ _]
   (ajax/get-request "/api/data"
                     [:fetch-data/success]
                     [:generic-failure])))

(rf/reg-event-db
 :fetch-data/success
 (fn [db [_ result]]
   (assoc db :data (walk/keywordize-keys result))))

;;SSE

(defn json->cljs
  [json]
  (js->clj (.parse js/JSON json) :keywordize-keys true))

(rf/reg-event-fx
 ::handle-event
 (fn [{:keys [db]} [_ e]]
   (let [data (dissoc (json->cljs (.-data e)) :audience)
         message (:message data)
         event (cond
                 (string? message)
                 (keyword message)
                 (vector? message)
                 (apply keyword message)
                 :else
                 ;;do nothing
                 :heartbeat)]
     (when (not= event :heartbeat)
       {:dispatch [event (:data data)]}))))

(rf/reg-event-fx
 ::sse-close
 (fn [{:keys [db]} _]
   (let [es (:event-source db)]
     (.close es)
     {})))

(rf/reg-event-fx
 ::sse-listen
 (fn [{:keys [db]} _]
   (when-let [es (:event-source db)]
     (.close es))
   (let [es (js/EventSource. "/events")]
     (.addEventListener es "message" #(rf/dispatch [::handle-event %]))
     {:db (assoc db :event-source es)})))
