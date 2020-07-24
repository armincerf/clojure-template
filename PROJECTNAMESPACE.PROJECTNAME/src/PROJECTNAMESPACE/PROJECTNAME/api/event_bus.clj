(ns PROJECTNAMESPACE.PROJECTNAME.api.event-bus
  (:require [clojure.tools.logging :as log]
            [manifold.bus :as bus]
            [integrant.core :as ig]))

(defmethod ig/init-key :PROJECTNAMESPACE.PROJECTNAME.api/event-bus
  [_ _]
  (log/info "starting event bus")
  (bus/event-bus))
