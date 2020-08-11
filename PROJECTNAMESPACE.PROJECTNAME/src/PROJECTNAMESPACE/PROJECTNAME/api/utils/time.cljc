(ns PROJECTNAMESPACE.PROJECTNAME.api.utils.time
  (:refer-clojure :exclude [<= >= > <])
  (:require #?(:clj [net.danielcompton.defn-spec-alpha :as dfs])
            #?(:clj [clojure.instant])
            [tick.alpha.api :as tick])
  #?(:clj (:import (java.time Instant Period ZonedDateTime ZoneOffset))))

#?(:clj
   (defn instant? [x]
     (instance? Instant x)))

(defn local-time
  [t]
  (tick/date-time
    (tick/in (tick/instant t) "Europe/London")))

(defn time-before?
  "Returns true if t1 is before t2, t2 defaults to now."
  ([t1] (time-before? t1 (tick/instant)))
  ([t1 t2]
   (tick/< (local-time t1) (local-time t2))))

(defn <=
  "Returns true if times are in non-decreasing order."
  [t1 t2 & more]
  (apply tick/<= (map local-time (into [t1 t2] more))))

(defn time-after?
  "Returns true if t1 is after t2, t2 defaults to now."
  ([t1] (time-after? t1 (tick/instant)))
  ([t1 t2]
   (tick/> (local-time t1) (local-time t2))))

(defn >=
  "Returns true if times are in non-increasing order"
  ([t1 t2 & more]
   (apply tick/>= (map local-time (into [t1 t2] more)))))

(defn time-between?
  "Returns true if t is after start and before end, defaults to inclusive bounds."
  ([t start end]
   (time-between? t start end true true))
  ([t start end start-inclusive? end-inclusive?]
   (let [[t start end] (map local-time [t start end])]
     (cond (and start-inclusive? end-inclusive?) (<= start t end)
           start-inclusive? (and (<= start t) (time-after? end t))
           end-inclusive? (and (time-before? start t) (<= t end))
           :else (and (time-before? start t) (time-after? end t))))))

(defn add
  "Add duration unit (defaults to minutes) to an instant (now, by default).
  Duration units available are :millis :seconds :minutes :hours :days Returns
  `java.util.Date.`"
  ([minutes] (add minutes {:now (tick/instant) :duration :minutes}))
  ([amount {:keys [now duration] :or {duration :minutes}}]
   (->> duration
        (tick/new-duration amount)
        (tick/+ (or now (tick/instant))))))

(defn subtract
  "Subtract duration unit (defaults to minutes) from an instant (now, by default).
  Duration units available are :millis :seconds :minutes :hours :days Returns
  `java.util.Date.`"
  ([minutes] (subtract minutes {:now (tick/instant) :duration :minutes}))
  ([amount {:keys [now duration] :or {duration :minutes}}]
   (->> duration
        (tick/new-duration amount)
        (tick/- (or now (tick/instant))))))

(defn millis-from-epoch
  "takes an instant and returns the number of milliseconds between the epoch and
  that instant"
  [t]
  (tick/millis
    (tick/duration
      {:tick/beginning (tick/epoch)
       :tick/end t})))

#?(:clj
   (dfs/defn to-sql-time
     [t :- inst?]
     (new java.sql.Timestamp (millis-from-epoch (tick/instant t)))))

#?(:clj
   (dfs/defn from-iso8601-period-string
     [iso-string :- string?]
     (Period/parse iso-string)))

#?(:clj
   (defn now-sql-time
     []
     (to-sql-time (tick/now))))

#?(:clj
   (dfs/defn period-end :- instant?
     "Calculate the end of the period, given a start and the recurring time duration"
     [period-start :- instant?
      recurring-period :- :iso8601/duration]
     ;; This looks convoluted, but there is a good reason for it.
     ;; We want to calculate the end time given the start (an Instant), and a period string.
     ;; You can add a period of days to an Instant, but not any larger ChronoUnit. This is
     ;; because different calendar systems can define weeks, months, and years differently
     ;; and Instant is calendar agnostic.
     ;; A UTC ZonedDateTime uses the ISO calendar system, and lets you do calculations with
     ;; larger periods. A UTC ZonedDateTime can be converted to/from the same point in time,
     ;; as an Instant but is more 'localized' to the ISO calendar system.
     ;; Instants are more convenient to work with, so we prefer to deal with them in the rest
     ;; of our system.
     ;; https://stackoverflow.com/q/54851282
     ;; Convert from an Instant to a UTC ZonedDateTime to add the period
     (let [zdt-period-start (ZonedDateTime/from (.atZone period-start ZoneOffset/UTC))]
       (-> (tick/+
            zdt-period-start
            (from-iso8601-period-string recurring-period))
           ;; Convert back to an Instant on the way out
           (Instant/from)))))

(defn start-of-today
  "Returns an ISO string representing today at 00:00:00hrs"
  []
  #?(:clj (-> (tick/today)
              str
              clojure.instant/read-instant-date)
     :cljs (-> (js/Date.)
               (.setHours 0 0 0 0)
               js/Date.
               .toISOString)))

#?(:clj
   (defn current-period-start
     "Given a period (e.g. P7D, 7 days) and a start date, finds the index and start date of the period in
      which `now` falls. Returns `[-1 nil]` if `now` is before the start date or equal to / after the end
      date."
     [now period start-at & [end-at]]
     (if (if end-at
           (time-between? now start-at end-at true false)
           (<= start-at now))
       (loop [index 0
              start-at start-at]
         (let [next-start-at (period-end (tick/instant start-at) period)]
           (if (time-before? now next-start-at)
             [index start-at]
             (recur (inc index) next-start-at))))
       [-1 nil])))
