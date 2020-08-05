(ns PROJECTNAMESPACE.PROJECTNAME.frontend.events
  (:require [re-frame.core :as rf]
            [PROJECTNAMESPACE.PROJECTNAME.frontend.http :as http]
            [re-pressed.core :as rp]))

(def debug?  ^boolean goog.DEBUG)

(rf/reg-event-fx
 :init
 (fn [_ _]
   (js/console.log "Dispatching init handlers")
   {:dispatch-n [[::http/me]
                 [::rp/set-keydown-rules
                  {:event-keys [(when debug?
                                  [;; dispatch :debug-toggle if
                                   [:debug-toggle]
                                   ;; ctrl + del key are pressed
                                   [{:keyCode 46
                                     :ctrlKey true}]])]
                   :clear-keys
                   ;; will clear the previously recorded keys if
                   [;; escape
                    [{:keyCode 27}]
                    ;; or Ctrl+g are pressed
                    [{:keyCode   71
                      :ctrlKey true}]]}]
                 [::http/sse-listen]]
    :db {:debug? debug?
         :show-menu? false}}))

(rf/reg-event-db
 :debug-toggle
 (fn [db _]
   (update db :debug? not)))

(rf/reg-event-db
 :menu-toggle
 (fn [db _]
   (update db :show-menu? not)))

(rf/reg-event-db
 :modal
 (fn [db [_ data]]
   (assoc db :modal data)))

(rf/reg-event-db
 :close-modal
 (fn [db _]
   (dissoc db :modal-component)))

(rf/reg-event-fx
 :global-message/add
 (fn [{:keys [db]} [_ message]]
   {:db (assoc db :global-message message)
    :dispatch-later [{:ms 3000
                      :dispatch [:global-message/dismiss]}]}))

(rf/reg-event-db
 :global-message/dismiss
 (fn [db _]
   (dissoc db :global-message)))

