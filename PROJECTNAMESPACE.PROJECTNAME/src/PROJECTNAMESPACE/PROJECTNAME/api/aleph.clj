(ns PROJECTNAMESPACE.PROJECTNAME.api.aleph
  (:require [aleph.http :as aleph]
            [clojure.tools.logging :as log]
            [integrant.core :as ig]))

(defmethod ig/init-key :PROJECTNAMESPACE.PROJECTNAME.api/aleph [_ {:keys [handler] :as opts}]
  (let [handler (atom (delay (:handler opts)))
        options (dissoc opts :handler)]
    (log/info "starting server" (select-keys opts [:port]))
    {:handler handler
     :server  (aleph/start-server (fn [req] (@@handler req)) options)}))

(defmethod ig/halt-key! :PROJECTNAMESPACE.PROJECTNAME.api/aleph [_ {:keys [server]}]
  (log/info "Stopping server")
  (.close ^java.io.Closeable server))

(defmethod ig/suspend-key! :PROJECTNAMESPACE.PROJECTNAME.api/aleph [_ {:keys [handler]}]
  (reset! handler (promise)))

(defmethod ig/resume-key :PROJECTNAMESPACE.PROJECTNAME.api/aleph [key opts old-opts old-impl]
  (if (= (dissoc opts :handler) (dissoc old-opts :handler))
    (do (deliver @(:handler old-impl) (:handler opts))
        old-impl)
    (do (ig/halt-key! key old-impl)
        (ig/init-key key opts))))
