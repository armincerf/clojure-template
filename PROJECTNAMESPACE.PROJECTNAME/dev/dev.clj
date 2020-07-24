(ns dev
  (:require
   [crux.api :as crux]
   [dev-extras :refer :all]))

;; Add your helpers here
(defn crux []
  (:juxt.crux.ig/system system))
