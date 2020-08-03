(ns PROJECTNAMESPACE.test.utils
  (:require [integrant.core :as ig]
            [reitit.core :as r]
            [clojure.tools.logging :as log]
            [PROJECTNAMESPACE.PROJECTNAME.api.fixtures :as f :refer [*system*]]))

(defn system-lookup
  [key]
  (fn []
    (if-let [[k v] (ig/find-derived-1 *system* key)]
      (ig/resolve-key k v)
      (throw (ex-info (format "Couldn't find a %s in the system" key)
                      {:key key})))) )

(def test-app (system-lookup :PROJECTNAMESPACE.PROJECTNAME.api/ring))
(def test-db (system-lookup :juxt.crux.ig/system))
(def test-origin (system-lookup :PROJECTNAMESPACE.PROJECTNAME.api/origin))
(def router (system-lookup :PROJECTNAMESPACE.PROJECTNAME.api/router))

(defn submap?
  "Is m1 a subset of m2?"
  [m1 m2]
  (if (and (map? m1) (map? m2))
    (every? (fn [[k v]] (and (contains? m2 k)
                             (submap? v (get m2 k))))
            m1)
    (= m1 m2)))

(defn merge-params [req]
  (assoc req :params (merge (:params req)
                            (:form-params req)
                            (:query-params req)
                            (:body-params req))))

(defn request-to
  "Takes a request map and a namespaced keyword route name or a string route path.

   e.g. (request-to ::expenses/expense-find-all {:request-method :get})
        (request-to '/login' {:request-method :get})

   Request defaults to {:request-method :get} if not provided."
  ([route]
   (request-to route {}))
  ([route request]
   (request-to route request *system*))
  ([route request system]
   (let [app (val (ig/find-derived-1 system :PROJECTNAMESPACE.PROJECTNAME.api/ring))
         router (val (ig/find-derived-1 system :PROJECTNAMESPACE.PROJECTNAME.api/router))
         match (when (keyword? route)
                 (r/match-by-name router route (get request :path-params {})))
         route-path (if (keyword? route)
                      (r/match->path match)
                      route)]
     ;; First check if the route exists at all
     (when (and (keyword? route) (nil? match))
       (throw (ex-info "Couldn't find match for route name" {:route-name route})))
     ;; Then check if we could create a path for the route
     (when (nil? route-path)
       (throw (ex-info "Couldn't create a path for route, check path params"
                       {:route-name route
                        :path-params (get request :path-params)
                        :template (:template match)})))
     (log/info "requesting")
     (-> request
         merge-params
         (update :scheme #(or % :https))
         (update :request-method #(or % :get))
         (assoc :host "localhost"
                :uri route-path)
         app
         deref))))
