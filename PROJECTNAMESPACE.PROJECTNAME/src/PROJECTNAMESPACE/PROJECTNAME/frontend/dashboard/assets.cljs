(ns PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.assets
  (:require [PROJECTNAMESPACE.PROJECTNAME.frontend.uikit.table :as table]
            [PROJECTNAMESPACE.PROJECTNAME.frontend.dashboard.subscriptions :as sub]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [PROJECTNAMESPACE.PROJECTNAME.frontend.common :as common]
            [tick.alpha.api :as t]))

(defn page
  []
  (let [page-title @(rf/subscribe [::sub/page-title])]
    [:<>
     [:h1.text-lg page-title]
     [:div.component-card
      [table/table
       {:columns [{:column-key :status
                   :column-name "Status"
                   :render-only #{:sort :filter}
                   :render-fn
                   (fn [_ v]
                     (let [[icon-class span-class]
                           (case v
                             "Success" ["fa-check-circle"
                                        "color-success"]
                             "Warning" ["fa-exclamation-circle"
                                        "color-warning"]
                             "Error" ["fa-stop-circle"
                                      "color-error"]
                             nil)]
                       [:div.flex.items-center
                        [:span
                         {:class span-class
                          :style {:font-size "1.2rem"}}
                         [:i.fas.padding-right-sm
                          {:class icon-class}]]
                        [:span v]]))}
                  {:column-key :name
                   :column-name "Name"
                   :render-only #{:sort :filter}
                   :render-fn (fn [_ v] [:span (str "Item" v)])}
                  {:column-key :level
                   :column-name "Level"}
                  {:column-key :bool
                   :column-name "Bool"}
                  {:column-key :notification
                   :column-name "Notification"}
                  {:column-key :time
                   :column-name "Time"}
                  {:column-key :date
                   :column-name "Date"}]
        :filters [{:label "status"
                   :column-key :status}
                  {:label "Name"
                   :column-key :name}
                  {:label "Bool"
                   :column-key :bool}
                  {:label "Notification"
                   :column-key :notification}]
        :rows (map-indexed (fn [idx]
                             {:id idx
                              :status (rand-nth ["Error" "Warning" "Success"])
                              :name (inc idx)
                              :level (rand-nth ["Primary"
                                                "Secondary"])
                              :bool (rand-nth ["Yes" "No"])
                              :notification (rand-nth ["Message here" "Another message here"])
                              :time (str (t/format (t/formatter "HH:mm")(t/time)))
                              :date (str (t/date))})
                           (range 16))}]]]))
