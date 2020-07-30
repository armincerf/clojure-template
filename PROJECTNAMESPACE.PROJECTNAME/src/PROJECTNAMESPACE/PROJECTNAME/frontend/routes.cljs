(ns PROJECTNAMESPACE.PROJECTNAME.frontend.routes)

(def dashboard-pages
  [{:path "/"
    :name :homepage
    :link-text "Home"
    :icon "fa-home"}
   {:path "/assets"
    :icon "fa-briefcase"
    :name :assets
    :link-text "Assets"}
   {:path "/customers"
    :icon "fa-users"
    :name :customers
    :children [:customer]
    :link-text "Customers"
    :dispatch [:dashboard/customers-fetch]}
   {:path "/customers/:customer"
    :name :customer
    :link-text "Customer Profile"}
   {:path "/customers/:customer/assets/:asset"
    :name :customer-asset
    :link-text "Asset"}
   {:path "/assets/:asset"
    :name :assets-asset
    :link-text "Assets"}
   {:path "/formats"
    :name :formats
    :link-text "Formats"}
   {:path "/features"
    :name :features
    :link-text "Features"}
   {:path "/settings"
    :name :settings}])

(defonce routes
  ["/dashboard"
   (for [{:keys [path] :as page} dashboard-pages]
     [path page])])
