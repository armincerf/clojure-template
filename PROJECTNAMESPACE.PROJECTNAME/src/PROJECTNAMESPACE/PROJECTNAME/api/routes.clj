(ns PROJECTNAMESPACE.PROJECTNAME.api.routes
  (:require [clojure.spec.alpha :as s]
            [crux.api :as crux]
            [hiccup.page :as hiccup]
            [ring.util.http-response :refer [see-other]]
            [integrant.core :as ig]
            [PROJECTNAMESPACE.PROJECTNAME.api.errors :as errors]
            [PROJECTNAMESPACE.PROJECTNAME.api.auth.authorization-rules :as rules]
            [PROJECTNAMESPACE.PROJECTNAME.api.cookies :as cookies]
            [PROJECTNAMESPACE.PROJECTNAME.api.app.routes :as app]
            [PROJECTNAMESPACE.PROJECTNAME.api.session :as session]
            [PROJECTNAMESPACE.PROJECTNAME.api.dashboard.routes :as dashboard]
            [PROJECTNAMESPACE.PROJECTNAME.api.customers.routes :as customers]
            [PROJECTNAMESPACE.PROJECTNAME.api.customers.model :as customers.model]
            [PROJECTNAMESPACE.PROJECTNAME.api.customers.domain :as customers.domain]
            [PROJECTNAMESPACE.PROJECTNAME.api.breaches.routes :as breaches]
            [PROJECTNAMESPACE.PROJECTNAME.api.assets.routes :as assets]
            [reitit.ring :as ring]
            [ring.util.http-response :as response]
            [PROJECTNAMESPACE.PROJECTNAME.api.spec :as spec]
            [PROJECTNAMESPACE.PROJECTNAME.api.sse :as sse]
            [spell-spec.alpha :as spell]
            [clojure.java.io :as io]
            [clojure.string :as str]))

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
    ["/" {:middleware middleware
          :authorize rules/open}
     ["login" {:name :login
               :get {:handler (fn handle-login
                                [req]
                                (-> (response/ok (slurp (io/resource "signup.html")))
                                    (response/content-type "text/html")))}
               :post {:parameters {:form {:email ::spec/email
                                          :password ::spec/non-blank-string}}
                      :handler (fn login-user
                                 [req]
                                 (def req req)
                                 (let [db (crux/db node)
                                       _ (def db db)
                                       {:keys [email password]} (get-in req [:parameters :form])
                                       {:keys [crux.db/id] :as acct} (customers.model/find-by-email db email)
                                       err-info {:email email}
                                       correct-pw? true ;(pw/match? password-hasher password (:password acct))
                                       ]
                                   (cond
                                     (nil? acct)
                                     (throw (errors/exception :account/consumer-does-not-exist err-info))

                                     (not correct-pw?)
                                     (throw (errors/exception :account/invalid-credentials))

                                     (:suspended? acct)
                                     (throw (errors/exception :account/suspended err-info))

                                     :else
                                     {:user {:email email
                                             :account-type :consumer}})
                                   ;(password-reset/invalidate-consumer-token! db id)
                                   (-> (response/see-other "/app")
                                       (session/update! (:session req) {:login id
                                                                        :account-type :customer}))))}}]
     ["signup" {:name :signup
                :post {:parameters {:form {:email ::spec/email
                                           :name ::spec/non-blank-string
                                           :password ::spec/non-blank-string}}
                       :handler (fn [req]
                                  (let [id (customers.domain/create-customer-handler components req)]
                                    (-> (response/see-other "/app")
                                        (session/update! (:session req) {:login id :account-type :customer})
                                        (cookies/set-entitlements-cookie
                                         id
                                         "secret"))))}}]
     (dashboard/routes components)
     (app/routes components)
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
      {:authorize rules/any-role}
      (customers/routes components)
      (assets/routes components)
      (breaches/routes components)

      ["me"
       {:name ::me
        :get {:responses {200 {:name ::spec/non-blank-string}}
              :handler (fn [req]
                         (def req req)
                         (response/ok (:identity req)))}}]
      ["data"
       {:name ::ui-data-resource
        :get {:handler (fn [req]
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
