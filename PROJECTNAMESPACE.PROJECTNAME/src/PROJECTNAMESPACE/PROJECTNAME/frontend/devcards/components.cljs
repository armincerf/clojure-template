(ns PROJECTNAMESPACE.PROJECTNAME.frontend.devcards.components
  (:require
   [reagent.core :as r]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.components :as components]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.uikit.table :as table]
   [devcards.core :refer [defcard-rg]]))

(defcard-rg breadcrumb
  [:div {:style {:background "#424242"
                 :padding "1rem"}}
   [components/breadcrumb [{:label "dealers" :href "#"}
                           {:label "dealer 1" :href "#"}]]])

(defcard-rg table-test
  [:div
   [:h1 "Customer List"]
   [table/table
    {:columns [{:column-key :error
                :column-name "Error"
                :render-fn (fn [row v]
                             (let [[icon-class icon-color]
                                   (case v
                                     "ok" ["fa-check-circle" "#00e676"]
                                     "warn" ["fa-exclamation-circle" "#ffea00"]
                                     "severe" ["fa-stop-circle" "#ff1744"]
                                     nil)]
                               [:span
                                {:style {:color icon-color
                                         :font-size "1.5rem"}}
                                [:i.fas {:class icon-class}]]))
                :render-only #{:sort :filter}}
               {:column-key :company-name
                :column-name "Company Name"}
               {:column-key :location
                :column-name "Location"}
               {:column-key :primary-contact
                :column-name "Primary Contact"}
               {:column-key :contract
                :column-name "Contract"}
               {:column-key :records
                :column-name "Records"}]
     :rows (map-indexed (fn [idx]
                          {:id idx
                           :error (rand-nth ["ok" "warn" "severe"])
                           :company-name (rand-nth ["Big bobs windows"
                                                    "Little bobs doors"
                                                    "Fat stans fencing"])
                           :location (rand-nth ["111 Main St, City of Town, USA"
                                                "222 Main St, City of Town, USA"
                                                "333 Main St, City of Town, USA"])
                           :primary-contact (rand-nth ["John Smith 715 642-2111"
                                                       "John Smith 715 642-2222"
                                                       "John Smith 715 642-2333"])
                           :contract (rand-nth ["Yes" "No"])
                           :records "View Status"}) (range 20))}]])

