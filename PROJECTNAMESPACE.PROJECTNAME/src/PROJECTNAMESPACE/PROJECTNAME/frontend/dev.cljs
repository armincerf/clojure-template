(ns PROJECTNAMESPACE.PROJECTNAME.frontend.dev
  (:require
   ;;TODO what is this used for?
   [com.smxemail.re-frame-cookie-fx]))

;;TODO make a config.cljs for this stuff and put env in meta header
(goog-define dev? false)
(goog-define route "http://localhost:7200/")
