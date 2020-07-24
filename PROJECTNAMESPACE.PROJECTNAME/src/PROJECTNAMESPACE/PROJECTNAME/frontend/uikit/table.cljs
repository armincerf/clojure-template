(ns PROJECTNAMESPACE.PROJECTNAME.frontend.uikit.table
  (:require
   [clojure.pprint :as pprint]
   [re-frame.core :as rf]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.uikit.utils :as utils]
   [PROJECTNAMESPACE.PROJECTNAME.frontend.common :as common]
   [reagent.core :as r]))

(defn pprint-str
  [x]
  (with-out-str (pprint/pprint x)))

(defn pprint-state
  [x]
  [:code
   {:style {:text-align "left"}}
   [:pre (pprint-str x)]])

(defn table-filters
  [data table-atom paginated-rows]
  (let [filters (:filters data)]
    (into
     [:div.table__dropdown-filters-container
      [:div.table__dropdown-filter-container
       [:div.dropdown-filter__label
        [:p "search all"]]
       [:div.filter__search-all
        [:div.form-control-wrapper.form-control-wrapper--icon-left
         [:input.form-control
          {:type "text"
           :value (utils/filter-all-value @table-atom)
           :on-change #(utils/filter-all-on-change % table-atom)}]
         [:i.fas.fa-search.icon.color-secondary-lighter]]]]]
     (conj
      (mapv
       (fn [{:keys [label column-key]}]
         ^{:key (str label column-key)}
         [utils/component-hide-show
          (fn [active? ref-button ref-dropdown]
            [:div.table__dropdown-filter-container
             [:div.dropdown-filter__label
              [:p label]]
             [:button.btn.btn--subtle.dropdown-filter__button
              {:ref ref-button}
              [:p.dropdown-filter__selected
               (utils/column-select-label @table-atom column-key)]
              [:i.fas.fa-caret-down]]
             (into
              [:div
               {:ref ref-dropdown
                :class (if active?
                         "table__dropdown-filter--show"
                         "table__dropdown-filter--hide")}]
              (mapv
               (fn [[id value processed-value]]
                 ^{:key (str id value)}
                 [:div.dropdown-filter__element
                  {:on-click #(utils/column-select-filter-on-change table-atom column-key value processed-value)}
                  [:input.checkbox
                   {:type "checkbox"
                    :checked (utils/column-select-filter-value @table-atom column-key value processed-value)
                    :value value
                    :on-change #()}]
                  [:label processed-value]])
               (utils/column-select-filter-options data column-key)))])]) filters)
      (when filters
        [:div.table__dropdown-filter-container
         [:div.dropdown-filter__label
          ;; needed for spacing consistency
          [:p.opacity-0 "Clear"]]
         [:button.btn.btn--subtle.dropdown-filter__button
          {:class (when (utils/filters-unselected? @table-atom)
                    "btn--disabled")
           :on-click #(utils/column-filter-reset-all table-atom)}
          [:i.fas.fa-ban.color-primary.margin-right-xxs]
          [:p.dropdown-filter__selected "Clear Filters"]]])))))

(defn header-columns
  [data table-atom]
  (let [columns (utils/table-columns data @table-atom)
        active-sort-column (ffirst (get-in @table-atom [:utils :sort]))]
    [:thead.table-header
     (into [:tr.table-header__tr]
           (mapv
            (fn [{:keys [column-key column-name]}]
              ^{:key column-key}
              [:th.table-header__th
               {:class (when (= column-key active-sort-column)
                         "table-header__th--active-sort")
                :on-click #(utils/column-sort table-atom
                                              column-key)}
               [:div.table-header__th-content
                [:p.table-header__th-column column-name]
                [:i.fas
                 ;; sort table by column inc or dec order
                 {:class (utils/column-sort-icon @table-atom
                                                 column-key)}]]])
            columns))]))

(defn loading-table
  [data table-atom {:keys [rows cols]}]
  (let [columns (:columns data)]
    [:div.table__main
     [:table.table
      (if columns
        [header-columns data table-atom]
        [:thead
         [:tr
          (for [col (range cols)]
            ^{:key col}
            [:th [:span]])]])
      [:tbody.table__body
       (for [row (range rows)]
         ^{:key row}
         [:tr.loading
          (for [col (range (or (count columns) cols))]
            ^{:key col}
            [:td.td-loading-bar
             [:span.loading-bar__span]])])]]]))

(defn body-rows
  [data table-atom rows]
  (let [columns (utils/table-columns data @table-atom)]
    (if (seq rows)
      [:tbody.table-body
       (for [row rows]
         ^{:key row}
         [:tr.table__row
          (when (:row-link data)
            (let [href ((get-in data [:row-link :href]) row)]
              {:class "cursor-pointer"
               :on-click #(let [route-info (common/url->route href)
                                name (get-in route-info [:data :name])
                                path-params (get-in route-info [:path-params])
                                query-params (get-in route-info [:query-params])]
                            (rf/dispatch [:navigate name path-params query-params]))}))
          (for [{:keys [column-key render-fn]} columns]
            ^{:key (str row column-key)}
            [:td.padding-sm
             (if render-fn
               (render-fn row (column-key row))
               (column-key row))])])]
      [:tbody.table__body.table__no-data
       [:tr [:td.td__no-data
             "Nothing to show"]]])))

(defn filter-all
  [table-atom]
  [:div.top__filter-all
   [:input.input.input--side-icons.input__no-borders
    {:value (utils/filter-all-value @table-atom)
     :on-change #(utils/filter-all-on-change % table-atom)}]
   [:span.input__icon.input__left-icon
    [:i.fas.fa-search]]
   (when (not-empty (utils/filter-all-value @table-atom))
     [:span.input__icon.input__right-icon.input__icon--clickable
      {:on-click #(utils/filter-all-reset table-atom)}
      [:i.fas.fa-times]])])

(defn pagination
  [table-atom processed-rows]
  [:table.table__pagination
   [:tfoot
    [:tr
     [:td.table__pagination-td
      [utils/select
       (fn [active? ref-button ref-dropdown]
         [:div.pagination__select
          [:button.btn.btn--subtle
           {:ref ref-button}
           (utils/pagination-rows-per-page @table-atom)
           [:i.fas.fa-caret-down.margin-left-xxs]]
          [:div.select
           {:ref ref-dropdown
            :class (if active?
                     "select--show"
                     "select--hide")}
           (let [on-click #(utils/pagination-rows-per-page-on-change % table-atom)]
             [:<>
              [:option.option
               {:value "10"
                :on-click on-click}
               (str "10" " rows")]
              [:option.option
               {:value "50"
                :on-click on-click}
               (str "50" " rows")]
              [:option.option
               {:value "100"
                :on-click on-click}
               (str "100" " rows")]])]])]
      [:div.pagination__info
       (utils/pagination-current-and-total-pages @table-atom
                                                 processed-rows)]
      [:div.pagination__arrow-group
       [:div.pagination__arrow-nav
        {:class (when (<= (utils/pagination-current-page @table-atom) 0)
                  "pagination__arrow-nav--disabled")
         :on-click #(utils/pagination-dec-page table-atom)}
        [:i.fas.fa-chevron-left]]
       [:div.pagination__arrow-nav
        {:class (when (utils/pagination-rows-exhausted? @table-atom
                                                        processed-rows)
                  "pagination__arrow-nav--disabled")
         :on-click #(utils/pagination-inc-page table-atom
                                               processed-rows)}
        [:i.fas.fa-chevron-right]]]]]]])

(defn table
  [data]
  (let [table-atom (r/atom {:utils (dissoc data :columns :rows :filters)})]
    (fn [data]
      (let [[processed-rows paginated-rows] (utils/process-rows data @table-atom)]
        [:div.uikit-table
         #_[:pre (with-out-str (pprint/pprint @table-atom))]
         [table-filters data table-atom paginated-rows]
         [:div.table-container
          [:table.table
           [header-columns data table-atom]
           [body-rows data table-atom paginated-rows]]]
         [pagination table-atom processed-rows]]))))
