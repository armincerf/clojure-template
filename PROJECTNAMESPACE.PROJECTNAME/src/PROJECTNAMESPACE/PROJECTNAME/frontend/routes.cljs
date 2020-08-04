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
    :icon "G"
    :name :app/homepage
    :link-text "Overview"}
   {:path "/add-data"
    :icon "z"
    :name :app/add-data
    :link-text "Add Data"}
   {:path "/account-settings"
    :icon "x"
    :name :app/account-settings
    :link-text "Account Settings"}])

(defonce routes
  ["/"
   ["app"
    (for [{:keys [path] :as page} app-pages]
      [path page])]
   ["dashboard"
    (for [{:keys [path] :as page} dashboard-pages]
      [path page])]])
