(ns PROJECTNAMESPACE.PROJECTNAME.api.sse
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [manifold.bus :as bus]
            [manifold.deferred :as deferred]
            [manifold.stream :as stream]))

(defn stream-msg [payload]
  (str "data:" (json/encode payload) "\n\n"))

(defn audience-filter-xf
  [user]
  (filter #(if-let [aud (:audience %)]
             (aud user)
             %)))

(defn publish-global-event
  "Given a manifold bus which the event stream subscribes to, publishes 'event' as
  a global message."
  [bus event]
  (if-let [event-bus bus]
    (try
      (deferred/timeout! (bus/publish! event-bus :global (stream-msg event)) 1000)
      (catch Exception e
        (log/error e "error publishing global event"
                       {:event event
                        :bus event-bus})))
    (log/error "bus is nil!")))

(defn sse-handler
  [{:keys [event-bus] :as components} req]
  (log/info "SSE connected")
  (let [user-id (get-in req [:identity :player-name])
        heartbeat-stream
        (stream/periodically 20000 (constantly (stream-msg {:message :heartbeat})))
        outstream (stream/stream 0)]
    (stream/connect heartbeat-stream outstream {:upstream? true})
    (stream/connect (bus/subscribe event-bus :global) outstream {:upstream? true})
    {:status 200
     :headers {"Content-Type" "text/event-stream"}
     :body outstream}))

(defn fetch-alerts!
  [event-bus]
  (publish-global-event event-bus {:message :PROJECTNAMESPACE.PROJECTNAME.frontend.http/fetch-data}))

(comment
  (require '[dev-extras])
  (def bus (:PROJECTNAMESPACE.PROJECTNAME.api/event-bus dev-extras/system))
  (publish-global-event bus {:message :alert :data "Test alert"}))
