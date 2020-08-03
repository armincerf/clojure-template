(ns PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.subscriptions
  (:require
   [reitit.frontend.easy :as reitit]
   [medley.core :as medley]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.table-utils :as table-utils]
   [PROJECTNAMESPACE.PROJECTNAME.common :as common]
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

(defn find-data-by-id
  [db k id]
  (let [data (get-in db [:data k])]
    (medley/find-first #(= id (name (keyword (:id %)))) data)))

(rf/reg-sub
 ::customer-profile
 (fn [db _]
   (let [id (get-in db [:current-route :path-params :customer])]
     (find-data-by-id db :dashboard/customers id))))
