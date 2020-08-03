(ns PROJECTNAMESPACE.PROJECTNAME.frontend.routes)

(def dashboard-pages
  [{:path "/"
    :name :dashboard/homepage
    :link-text "Home"
    :icon "fa-home"}
   {:path "/assets"
    :icon "fa-briefcase"
    :name :dashboard/assets
    :link-text "Assets"
    :dispatch-n [[:dashboard/data-fetch "/api/v1/assets"]
                 [:dashboard/data-fetch "/api/v1/customers"]]}
   {:path "/customers"
    :icon "fa-users"
    :name :dashboard/customers
    :children [:customer]
    :link-text "Customers"
    :dispatch [:dashboard/data-fetch "/api/v1/customers"]}
   {:path "/customers/:customer"
    :name :dashboard/customer
    :link-text "Customer Profile"
    :dispatch [:dashboard/data-fetch "/api/v1/customers"]}
   {:path "/customers/:customer/assets/:asset"
    :name :dashboard/customer-asset
    :link-text "Asset"}
   {:path "/assets/:asset"
    :name :dashboard/asset
    :link-text "Assets"
    :dispatch-n [[:dashboard/data-fetch "/api/v1/assets"]
                 [:dashboard/data-fetch "/api/v1/customers"]]}
   {:path "/formats"
    :name :dashboard/formats
    :link-text "Formats"}
   {:path "/features"
    :name :dashboard/features
    :link-text "Features"}
   {:path "/settings"
    :name :dashboard/settings}])

(def app-pages
  [{:path ""
    :name :app/homepage
    :link-text "Home"}])

(defonce routes
  ["/"
   ["app"
    (for [{:keys [path] :as page} app-pages]
      [path page])]
   ["dashboard"
    (for [{:keys [path] :as page} dashboard-pages]
      [path page])]])
