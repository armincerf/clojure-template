(ns PROJECTNAMESPACE.PROJECTNAME.frontend.devcards.main
  (:require
   PROJECTNAMESPACE.PROJECTNAME.frontend.devcards.components
   [devcards.core :as dc]))

(dc/start-devcard-ui!)

(comment
  (let [state (r/state nil)]
    (dc/defcard-rg counter
      [:div "Reagent Component"]
      state
      {
       :frame true         ;; whether to enclose the card in a padded frame
       :heading true       ;; whether to add a heading panel to the card
       :padding true       ;; whether to have padding around the body of the card
       :hidden false       ;; whether to diplay the card or not
       :inspect-data false ;; whether to display the data in the card atom
       :watch-atom true    ;; whether to watch the atom and render on change
       :history false      ;; whether to record a change history of the atom
       :classname ""       ;; provide card with a custom classname
       :projection identity ;; provide a projection function for card state
       }
      {:inspect-data true})))
