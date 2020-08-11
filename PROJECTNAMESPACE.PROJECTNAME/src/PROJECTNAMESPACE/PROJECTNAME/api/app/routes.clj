(ns PROJECTNAMESPACE.PROJECTNAME.api.app.routes
  (:require [ring.util.http-response :as response]
            [PROJECTNAMESPACE.PROJECTNAME.api.auth.authorization-rules :as rules]
            [hiccup.page :as hiccup]
            [clojure.java.io :as io]))

(defn- index-html
  "main index, serves the dashboard cljs bundle"
  [request]
  (hiccup/html5
   {:lang "en"}
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
    [:meta
     {:name "viewport"
      :content "width=device-width, initial-scale=1.0, maximum-scale=1.0"}]
    [:link {:rel "icon" :href "/img/dashboard-logo.svg" :type "image/x-icon"}]
    [:link#pagestyle {:href "/css/app.css" :rel "stylesheet" :type "text/css"}]
    [:title "PROJECTNAME"]
    (hiccup/include-css "/webjars/font-awesome/5.13.0/css/all.min.css")]
   [:body
    (hiccup/include-js "/app.js"
                       "/js/util.js"
                       "/js/scripts.min.js")]))

(defn routes
  [components]
  (let [handle-index (fn handle-index
                       [req]
                       (-> (response/ok
                           (slurp (io/resource "index.html")))
                           (response/content-type "text/html")))]
    ["app"
     {:authorize rules/customer-role}
     ;; TODO is there a better way to allow both /app and /app/* in reitit??
     [""  {:name :app-cljs-routes-root
           :get {:handler handle-index}}]
     ["/*" {:name :app-cljs-routes
            :get {:handler handle-index}}]]))
