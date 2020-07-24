(ns PROJECTNAMESPACE.PROJECTNAME.frontend.routes)

(def dashboard-pages
  [{:path "/"
    :name :homepage
    :link-text "Home"
    :icon "fa-home"}
   {:path "/assets"
    :icon "fa-balance-scale"
    :name :assets
    :link-text "Assets"}
   {:path "/customers"
    :icon "fa-users"
    :name :customers
    :link-text "Customers"}
   {:path "/customers/:customer"
    :name :customer
    :link-text "Customers"}
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
    :link-text "Features"}])

(defonce routes
  ["/dashboard"
   (for [{:keys [path name link-text]} dashboard-pages]
     [path
      {:name name
       :link-text link-text}])])
