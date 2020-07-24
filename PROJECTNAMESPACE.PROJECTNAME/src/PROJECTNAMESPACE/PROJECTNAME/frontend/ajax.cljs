(ns PROJECTNAMESPACE.PROJECTNAME.frontend.ajax
  (:require [ajax.core :as ajax]))

(defn request
  ([method uri on-success on-error]
   (request method uri on-success on-error nil))
  ([method uri on-success on-error params]
   {:http-xhrio
    (cond-> {:uri uri
             :method method
             :format (ajax/json-request-format)
             :response-format (ajax/json-response-format
                               {:keywords? true})
             :on-success on-success
             :on-failure on-error}
      params (assoc :params params))}))

(defn get-request
  [uri on-success on-error]
  (request :get uri on-success on-error))

(defn put-request
  [uri params on-success on-error]
  (request :put uri on-success on-error params))

(defn post-request
  [uri params on-success on-error]
  (request :post uri on-success on-error params))

(defn delete-request
  [uri on-success on-error]
  (request :delete uri on-success on-error))
