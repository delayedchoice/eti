(ns eti.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [eti.events]
              [eti.subs]
              [eti.routes :as routes]
              [eti.views :as views]
              [eti.config :as config]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/top-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [:collect-data])
  (dev-setup)
  (mount-root))
