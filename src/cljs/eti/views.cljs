(ns eti.views
    (:require [re-frame.core :as re-frame]))

(defn home-panel []
  (let [data (re-frame/subscribe [:proxy-data])]
    (fn []
      [:div (str @data) ])))

(defn- panels [panel-name]
  (case panel-name
    :home-panel [home-panel]
    [:div]))

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel :home-panel]
    (fn []
      [show-panel active-panel])))

(defn top-panel
  []
  (let [ready?  (re-frame/subscribe [:initialised?])]
    (if-not @ready?
      [:div "Initialising ..."]
      [main-panel])))
