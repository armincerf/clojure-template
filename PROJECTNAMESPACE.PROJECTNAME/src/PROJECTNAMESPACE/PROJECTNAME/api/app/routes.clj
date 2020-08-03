(ns PROJECTNAMESPACE.PROJECTNAME.api.app.routes
  (:require [ring.util.http-response :as response]
            [hiccup.page :as hiccup]))

(defn- index-html
  "main index, serves the app cljs bundle"
  [request]
  (hiccup/html5
   {:lang "en"}
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
    [:meta
     {:name "viewport"
      :content "width=device-width, initial-scale=1.0, maximum-scale=1.0"}]
    [:link {:rel "icon" :href "/img/app-logo.svg" :type "image/x-icon"}]
    [:link#pagestyle {:href "/app.css" :rel "stylesheet" :type "text/css"}]
    [:title "PROJECTNAME"]
    (hiccup/include-css "/webjars/font-awesome/5.13.0/css/all.min.css")]
   [:body
    [:div#app
     [:div.pageloader.is-active
      [:span.title "Loading..."]]]
    (hiccup/include-js "/app.js"
                       "/js/util.js"
                       "/js/scripts.min.js")]))

(defn routes
  [components]
  ["app"
   ;; TODO is there a better way to allow both /app and /app/* in reitit??
   [""  {:name :app-cljs-routes-root
         :get {:handler (fn handle-root-index
                          [req]
                          (-> (response/ok (index-html req))
                              (response/content-type "text/html")))}}]
   ["/*" {:name :app-cljs-routes
          :get {:handler (fn handle-index
                           [req]
                           (-> (response/ok (index-html req))
                               (response/content-type "text/html")))}}]])
