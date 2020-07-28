(ns PROJECTNAMESPACE.PROJECTNAME.frontend.subscriptions
  (:require
   [PROJECTNAMESPACE.PROJECTNAME.frontend.table-utils :as table-utils]
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
 :global-message
 (fn [db _] (:global-message db)))

(rf/reg-sub
 :loading?
 (fn [db _] (:loading? db)))

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
 ::table-data-by-id
 (fn [db _]
   (into {} (map (fn [x] {(:crux.db/id x) x}) (:data db)))))

(rf/reg-sub
 ::alerts-table
 (fn [_ _]
   [(rf/subscribe [:db])
    (rf/subscribe [::table-data-by-id])])
 (fn [[db mappified-data] _]
   (let [columns [{:column-key :dealership/name
                   :column-name "Dealership"
                   :render-fn
                   (fn [m v] (table-utils/link-component
                              (str "/dashboard/dealership/" (:dealership/id m)) v))
                   :render-only #{:filter :sort}}
                  {:column-key :customer/name
                   :column-name "Customer"
                   :render-fn
                   (fn [m v] (table-utils/link-component
                              (str "/dashboard/dealership/" (:dealership/id m)
                                   "/customer/" (:customer/id m)) v))
                   :render-only #{:filter :sort}}
                  {:column-key :alert/action
                   :column-name "Type"
                   :render-fn table-utils/icon-component
                   :render-only #{:filter :sort}}
                  {:column-key :alert/date
                   :column-name "Date"
                   :render-fn table-utils/date-component
                   :render-only #{:sort}}]
         alerts (filter #(= "alert" (:PROJECTNAMESPACE/type %)) (:data db))
         rows (map (fn [alert]
                     (let [logical-device-id (:createdBy alert)
                           logical-device (get mappified-data logical-device-id)
                           logical-gateway-id (:logicalGateway logical-device)
                           logical-gateway (get mappified-data logical-gateway-id)
                           customer-id (:customer logical-gateway)
                           customer (get mappified-data customer-id)
                           dealership-id (:servicedByDealership customer)
                           dealership (get mappified-data dealership-id)]
                       {:alert/id (:crux.db/id alert)
                        :alert/date (:date alert)
                        :alert/action (:action alert)
                        :dealership/name (:name dealership)
                        :customer/name (:name customer)
                        :dealership/id (:crux.db/id dealership)
                        :customer/id (:crux.db/id customer)}))
                   alerts)]
     {:columns columns
      :rows rows
      :filters {:left
                {:input {:alert/date {:label "Date"}}
                 :select {:dealership/name {:label "Dealership"}
                          :customer/name {:label "Customer"}
                          :alert/action {:label "Alert"}}}}
      :sort {:alert/date :desc}})))
