(ns PROJECTNAMESPACE.PROJECTNAME.api.middleware.logging
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [lambdaisland.deep-diff :as ddiff]
            [lambdaisland.deep-diff.printer :as printer]
            [puget.color :as color]
            [reitit.core :as r]))

(defn wrap-log-to-console [handler ^Throwable e {:keys [uri request-method] :as req}]
  (log/errorf e "%s %s => %s" request-method (pr-str uri) "=>" (.getMessage e))
  (handler e req))

;; custom pretty print implementation

(def printer
  (-> (printer/puget-printer)
      (assoc :width 70)
      (update :color-scheme merge {:middleware [:blue]
                                   :handler [:green]
                                   :header [:green]
                                   :status [:blue]})))

(defn pad-stage [x]
  (format "%-15s" x))

(defn diff-doc [stage name previous current]
  (let [print-diff? (or (nil? name) (not= previous current))]
    [:group
     [:span "--- " (pad-stage stage)
      (when name
        (color/document printer :middleware (str " " name " ")))
      "---"
      (when print-diff? :break)]
     (when print-diff?
       [:nest (printer/format-doc (if previous
                                    (ddiff/diff previous current)
                                    current)
                                  printer)])]))

(defn not-map-doc [stage name obj]
  [:group
   [:span "--- " (pad-stage stage)
    (when name
      (color/document printer :middleware (str " " name " ")))
    "---"
    :break]
   [:nest (str (type obj) " ") (printer/format-doc (pr-str obj) printer)]])

(defn polish [request]
  (dissoc request ::r/match ::r/router ::original ::previous))

(defn -async
  [k async?]
  (keyword (if async? "async" "sync") (name k)))

(defn printed-request [name {::keys [previous] :as request} & [async?]]
  (printer/print-doc (diff-doc (-async :request async?) name (polish previous) (polish request)) printer)
  (-> request
      (update ::original (fnil identity request))
      (assoc ::previous request)))

(defn printed-response [name {::keys [previous] :as response} & [async?]]
  (if (map? response)
    (do (printer/print-doc (diff-doc (-async :response async?) name (polish previous) (polish response)) printer)
        (-> response
            (update ::original (fnil identity response))
            (assoc ::previous response)
            (cond-> (nil? name) (dissoc ::original ::previous))))
    (do (printer/print-doc (not-map-doc (-async :response async?) name response) printer)
        response)))

(defn print-diff-middleware
  ([]
   (print-diff-middleware nil))
  ([{:keys [name]}]
   {:name ::diff
    :compile
    (fn [route-data _router-opts]
      (when-not (:no-diffs route-data)
        (fn [handler]
          (fn
            ([request]
             (printed-response name (handler (printed-request name request))))
            ([request respond raise]
             (handler (printed-request name request true)
                      (comp respond #(printed-response name % true))
                      raise))))))}))

(defn print-req-header [request]
  (printer/print-doc [:group [:break]
                      [:span "=== "
                       (color/document
                        printer
                        :header
                        [:span
                         (str/upper-case (name (:request-method request)))
                         " "
                         (:uri request)])]]
                     printer))

(defn print-req-footer [request start response]
  (printer/print-doc [:group
                      [:span "=== "
                       (color/document printer
                                       :status
                                       [:span (str (:status response))])
                       " "
                       (color/document
                        printer
                        :header
                        [:span
                         (str/upper-case (name (:request-method request)))
                         " "
                         (:uri request)])
                       " "
                       [:span
                        ;; TODO: changing to "elapsed:" messes up the printing, figure out why.
                        "elapsed "
                        (str (Math/round ^double
                                         (/ (- (System/nanoTime) start)
                                            1000000.0)))
                        "ms"]]
                      [:break]]
                     printer)
  response)

(def request-stats
  {:name ::request-stats
   :compile
   (fn [route-data _router-opts]
     (when-not (:no-diffs route-data)
       (fn [handler]
         (fn
           ([request]
            (print-req-header request)
            (let [start (System/nanoTime)
                  resp (handler request)]
              (print-req-footer request start resp)))
           ([request respond raise]
            (let [start (System/nanoTime)]
              (print-req-header request)
              (handler request (comp respond #(print-req-footer request start %)) raise)))))))})

(defn print-handler-header [req]
  (printer/print-doc [:group [:span
                              "--- "
                              (pad-stage :handler)
                              " "
                              (color/document printer :handler (:uri req))
                              " ---"]]
                     printer))

(def handler-header
  "Prints a header for the endpoint to delineate it from middleware"
  ;; TODO: get the name of the handler from somewhere
  ;; TODO: print out the response after the handler runs, rather than letting the first
  ;; middleware after the response print the diff.
  {:name ::handler-printer
   :compile
   (fn [route-data _router-opts]
     (when-not (:no-diffs route-data)
       (fn [handler]
         (fn
           ([request]
            (print-handler-header request)
            (handler request))
           ([request respond raise]
            (print-handler-header request)
            (handler request respond raise))))))})

(defn print-request-diffs
  "A middleware chain transformer that helps you trace request diffs.

  Supports four levels:
  :none - No effect
  :info - Logs basic request info (status, route)
  :debug - Logs request, response and request stats
  :trace - Above levels + every diff between all middleware transformation

  To disable, set `:no-diffs true` in the route data for the endpoints
  you would like to log."
  ([chain]
   (print-request-diffs :verbose chain))
  ([level chain]
   (case level
     :trace (conj (reduce (fn [chain mw]
                            (into chain [mw (print-diff-middleware (select-keys mw [:name]))]))
                          [request-stats (print-diff-middleware)]
                          chain)
                  handler-header)
     :debug (flatten [request-stats (print-diff-middleware) chain handler-header])
     :info (flatten [request-stats chain handler-header])
     :none chain)))
