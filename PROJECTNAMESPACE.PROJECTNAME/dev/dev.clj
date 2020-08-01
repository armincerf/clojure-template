(ns dev
  (:require
   [crux.api :as crux]
   [dev-extras :refer :all]))

;; Add your helpers here
(defn crux []
  (:juxt.crux.ig/system system))

(defn query
  [query]
  (let [result (crux/q (crux/db (crux)) query)]
    (if (= 1 (count (:find query)))
      (map first result)
      result)))

(defn insert!
  "Inserts data into crux, data can be either a map or a sequence of maps.
   Optionally takes a async? boolean, if true the function will immediately
  return the data without waiting for crux to confirm the transaction. Async? is
  false by default"
  ([data] (insert! data false))
  ([data async?]
   (if (seq data)
     (do (crux/submit-tx
          (crux)
          (if (map? data)
            [[:crux.tx/put
              data]]
            (vec (for [item data]
                   [:crux.tx/put item]))))
         (when-not async? (crux/sync (crux)))
         data)
     "Not transacting as data is not valid")))

(defn all-ids
  []
  (query '{:find [?e]
           :where [[?e :crux.db/id]]}))

(defn reset-dev-db
  "Resets the dev DB, keeping history intact. (to also delete history its easiest
  to simply delete the data directory)"
  []
  (let [node (crux)]
    (crux/submit-tx
     node
     (for [id (all-ids)]
       [:crux.tx/delete id]))))
