(ns PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.subscriptions
  (:require
   [reitit.frontend.easy :as reitit]
   [medley.core :as medley]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.table-utils :as table-utils]
   [PROJECTNAMESPACE.PROJECTNAME.common :as common]
   [re-frame.core :as rf]))

(defn- find-data-by-id
  [db k id]
  (let [data (get-in db [:data k])]
    (medley/find-first #(= id (name (keyword (:id %)))) data)))

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
 ::assets-table
 (fn [db _]
   (let [columns [{:column-key :asset/name
                   :column-name "Asset Name"}
                  {:column-key :asset/description
                   :column-name "Asset Description"}
                  {:column-key :asset/type
                   :column-name "Asset Type"}
                  {:column-key :asset/customer
                   :column-name "Asset Customer"
                   :render-fn (fn [row id]
                                [:a.font-bold.color-secondary
                                 {:href (reitit/href
                                         :customer
                                         {:customer (common/id-key id)})}
                                 (or (:customer/name
                                      (common/find-by-id
                                       (get-in db [:data :dashboard/customers])
                                       id))
                                     "View Customer")])}
                  {:column-key :id
                   :column-name "Profile"
                   :render-only #{:sort :filter}
                   :render-fn (fn [row id]
                                [:a.font-bold.color-secondary
                                 "View Asset"])}]]
     {:loading? (:loading? db)
      :columns columns
      :rows (get-in db [:data :dashboard/assets])
      :row-link {:href (fn [row] (reitit/href :asset {:asset (common/id-key (:id row))}))}
      :filters [{:label "Name"
                 :column-key :asset/name}
                {:label "Type"
                 :column-key :asset/type}
                {:label "Customer"
                 :column-key :asset/customer}]
      :sort {:alert/date :desc}})))

(rf/reg-sub
 ::asset-profile
 (fn [db _]
   (let [id (get-in db [:current-route :path-params :asset])]
     (find-data-by-id db :dashboard/assets id))))

(rf/reg-sub
 :dashboard/data
 (fn [db [_ key]]
   (get-in db [:data key])))

(rf/reg-sub
 ::customers-table
 (fn [db _]
   (let [columns [{:column-key :customer/name
                   :column-name "Customer Name"}
                  {:column-key :customer/email
                   :column-name "Primary Contact"}
                  {:column-key :id
                   :column-name "Profile"
                   :render-only #{:sort :filter}
                   :render-fn (fn [row id]
                                [:a.font-bold.text-decoration-none.color-secondary
                                 "View Profile"])}]]
     {:loading? (:loading? db)
      :columns columns
      :rows (get-in db [:data :dashboard/customers])
      :row-link {:href (fn [row] (reitit/href :customer {:customer (common/id-key (:id row))}))}
      :filters [{:label "Name"
                 :column-key :customer/name}
                {:label "Email"
                 :column-key :customer/email}]
      :sort {:alert/date :desc}})))

(rf/reg-sub
 ::customer-profile
 (fn [db [_ id]]
   (let [id (or (common/id-key id)
                (get-in db [:current-route :path-params :customer]))]
     (find-data-by-id db :dashboard/customers id))))
