(ns PROJECTNAMESPACE.PROJECTNAME.frontend.common
  (:require [PROJECTNAMESPACE.PROJECTNAME.frontend.navigation :as navigation]
            [fork.re-frame :as fork]
            [clojure.string :as str]
            [PROJECTNAMESPACE.PROJECTNAME.common :as common]
            [re-frame.core :as rf]
            [reitit.frontend :as reitit]
            [reitit.frontend.easy :as rfe]))

(defn route->url
  "k: page handler i.e. :entity
  params: path-params map
  query: query-params map"
  ([k]
   (route->url k nil nil))
  ([k params]
   (route->url k params nil))
  ([k params query]
   (rfe/href k params query)))

(defn url->route
  "url: abosolute string path"
  [url]
  (reitit/match-by-path navigation/router url))

(def speed 500)
(def moving-frequency 15)

(defn cur-doc-top []
  (+ (.. js/document -body -scrollTop) (.. js/document -documentElement -scrollTop)))

(defn element-top [elem top]
  (when elem
    (if (.-offsetParent elem)
      (let [client-top (or (.-clientTop elem) 0)
            offset-top (.-offsetTop elem)]
        (+ top client-top offset-top (element-top (.-offsetParent elem) top)))
      top)))

(defn scroll-to-id
  [elem-id]
  (when-let [elem (.getElementById js/document elem-id)]
    (let [hop-count (/ speed moving-frequency)
          doc-top (cur-doc-top)
          gap (/ (- (element-top elem -60) doc-top) hop-count)]
      (doseq [i (range 1 (inc hop-count))]
        (let [hop-top-pos (* gap i)
              move-to (+ hop-top-pos doc-top)
              timeout (* moving-frequency i)]
          (.setTimeout js/window (fn []
                                   (.scrollTo js/window 0 move-to))
                       timeout))))))

(defn auto-form
  [editable-fields]
  [fork/form {:path :form
              :form-id "form-id"
              :prevent-default? true
              :clean-on-unmount? true
              :on-submit #(rf/dispatch [:data/update %])
              :initial-values (medley.core/map-keys
                               common/keyword->string
                               editable-fields)}
   (fn [{:keys [values
                form-id
                handle-change
                handle-blur
                submitting?
                handle-submit
                reset]}]
     [:div.form
      [:form.profile-component__form
       {:id form-id
        :on-submit handle-submit}
       (for [input (map common/keyword->string (keys editable-fields))]
         ^{:key input}
         [:div.margin-bottom-sm
          [:label.form-label.form__label
           (str/capitalize input)]
          [:div.form-control-wrapper
           [:input.form-control
            {:type "text"
             :name input
             :value (values input)
             :on-change handle-change
             :on-blur handle-blur}]]])
       [:button.btn.btn--md.form__submit
        {:type "submit"
         :disabled submitting?}
        "Submit Form"]]])])
