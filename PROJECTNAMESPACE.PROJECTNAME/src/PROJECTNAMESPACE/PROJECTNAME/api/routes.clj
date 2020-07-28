(ns PROJECTNAMESPACE.PROJECTNAME.api.routes
  (:require [clojure.spec.alpha :as s]
            [crux.api :as crux]
            [hiccup.page :as hiccup]
            [integrant.core :as ig]
            [PROJECTNAMESPACE.PROJECTNAME.api.dashboard.routes :as dashboard.routes]
            [PROJECTNAMESPACE.PROJECTNAME.api.dashboard.customers.routes :as customers.routes]
            [reitit.ring :as ring]
            [ring.util.http-response :as response]
            [PROJECTNAMESPACE.PROJECTNAME.api.spec :as spec]
            [PROJECTNAMESPACE.PROJECTNAME.api.sse :as sse]
            [spell-spec.alpha :as spell]))

(defn- devcards-html
  "Serves devcards"
  [request]
  (hiccup/html5
   {:lang "en"}
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
    [:meta
     {:name "viewport"
      :content "width=device-width, initial-scale=1.0, maximum-scale=1.0"}]
    [:link {:rel "icon" :href "/favicon.ico" :type "image/x-icon"}]
    [:link#pagestyle {:href "/app.css"
                      :id "com-rigsomelight-devcards-addons-css"
                      :rel "stylesheet" :type "text/css"}]
    [:title "PROJECTNAME devcards"]
    (hiccup/include-css "/webjars/font-awesome/5.13.0/css/all.min.css")]
   [:body
    (hiccup/include-js "/frontend-devcards.js")]))

(s/def ::components
  (s/map-of keyword? some?))

(defmethod ig/pre-init-spec ::routes
  [_]
  (spell/keys :req-un [::components ::spec/middleware]))

(defmethod ig/init-key ::routes
  [_ {:keys [middleware components]
      :or {middleware []}}]
  (let [{:keys [node]} components]
    ["/" {:middleware middleware}
     (dashboard.routes/routes components)
     ["devcards" {:name :cljs-devcards
                  :get {:handler (fn handle-devcards
                                   [req]
                                   (-> (response/ok (devcards-html req))
                                       (response/content-type "text/html")))}}]
     ["webjars/*"
      {:get {:handler (ring/create-resource-handler
                       {:root "META-INF/resources/webjars"})
             :no-diffs true}}]
     ["events"
      {:get {:handler #(sse/sse-handler components %)
             :no-diffs true}}]
     ["api/v1/"
      (customers.routes/routes components)

      ["me"
       {:name ::me
        :get {:responses {200 {:name ::spec/non-blank-string}}
              :handler (fn [req]
                         (response/ok (:identity req)))}}]
      ["data"
       {:name ::ui-data-resource
        :get {:responses {200 seq?}
              :handler (fn [req]
                         (->> (crux/q
                               (crux/db node)
                               '{:find [?e]
                                 :where [[?e :crux.db/id _]]
                                 :full-results? true})
                              (mapcat identity)
                              response/ok))}}]
      ["delete-db"
       {:post {:name ::delete-db
               :responses {200 {:message ::spec/non-blank-string}}
               :handler (fn [req]
                          (let [all-ids (->> (crux/q
                                              (crux/db node)
                                              '{:find [?id]
                                                :where [[?id :crux.db/id _]]})
                                             (map first))]
                            (crux/submit-tx
                             node
                             (mapv
                              (fn [id]
                                [:crux.tx/delete id])
                              all-ids))
                            (response/ok {:message "All documents deleted from DB"})))}}]]]))