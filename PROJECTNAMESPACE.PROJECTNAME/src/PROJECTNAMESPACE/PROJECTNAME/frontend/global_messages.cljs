(ns PROJECTNAMESPACE.PROJECTNAME.frontend.global-messages
  (:require [re-frame.core :as rf]))

(defn- header
  [msg-type]
  (case msg-type
    :message/error "Sorry!"
    :message/success "Success!"
    :message/info "Heads up!"
    "Heads up!"))

(defn- message
  "Produces a global message of type 'msg-type'.

  `msg` can be:
    * a string, in which case a preset error header is used
    * a map with :header and :body fields. :body can be either a string
      or a Hiccup vector which will be passed straight through.
    * a keyword representing an ident of a message to display.
      See `ident->body`."
  [msg-type msg]
  (merge {:global-message/type msg-type}
         (cond
           (string? msg)
           {:content {:header (header msg-type) :body msg}}

           (map? msg)
           {:content msg}

           (keyword? msg)
           {:ident msg})))

(defn- ident->body
  ([ident] (ident->body ident nil))
  ([ident data]
   (case ident
     :user/password-reset-success "Your password has been reset.")))

(defn- body->hiccup
  [body]
  (cond
    (string? body)
    [:p body]

    :else
    body))

;; -----------------------------------------------------------------------------
;; Public API
;; -----------------------------------------------------------------------------

(defn error
  "Produces a global error message for the user.

  For the shape of 'msg', see `message`. "
  [msg]
  (message :message/error msg))

(defn success
  "Produces a global success message for the user.

  For the shape of 'msg', see `message`."
  [msg]
  (message :message/success msg))

(defn info
  "Produces a global informational message for the user.

  For the shape of 'msg', see `message`."
  [msg]
  (message :message/info msg))

;; -----------------------------------------------------------------------------
;; Component
;; -----------------------------------------------------------------------------

(defn toast
  []
  (let [global-message @(rf/subscribe [:global-message])]
    (when global-message
      (let [message-type (:global-message/type global-message)
            message-class (str "toast--"
                               (case message-type
                                 :message/error "error"
                                 :message/success "success"
                                 "info"))
            message-header (or (:header (:content global-message))
                               (header message-type))]
        [:div.toasts
         {:key "global-message"}
         [:div.toast
          {:class [message-class]}
          [:button.delete {:on-click #(rf/dispatch [:global-message/dismiss])}]
          [:div.toast.toast__body
           [:div.toast.toast__header message-header]
           (cond
             (some? (:body (:content global-message)))
             (let [body (:body (:content global-message))]
               (body->hiccup body))

             (some? (:ident global-message))
             (let [body (ident->body (:ident global-message))]
               (body->hiccup body))

             :else
             (do
               (js/console.warn "Unknown message:" (clj->js global-message))

               [:p "Looks like a server error occurred processing your
                  request. Our team has been notified of the issue. Please
                  try again later."]))]]]))))

