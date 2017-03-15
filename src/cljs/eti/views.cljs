(ns eti.views
    (:require [re-frame.core :as rf]))

(defn nav-bar []
 [:nav.navbar.navbar-inverse
  [:div.container-fluid
   [:div.navbar-header
    [:button.navbar-toggle
     {:type "button" :data-toggle "collapse" :data-target "#myNavbar"}
     [:span.icon-bar]
     [:span.icon-bar]
     [:span.icon-bar]]
    [:a.navbar-brand {:href "#"} "ETI"]]
   [:div.collapse.navbar-collapse {:id "myNavbar"}
    [:ul.nav.navbar-nav
     [:li.active [:a {:href "#"} "About"]]]]]])

(defn side-bar []
  (let [boxes (rf/subscribe [:proxy-data])]
    [:div.col-sm-4
     [:div.nav.nav-sidebar.sidebar
     (for [k  (keys @boxes)]
        ^{:key k}
        [:div
         [:p [:a.detail-entry {:data-toggle "tooltip"
                               :data-container "body"
                               :title k
                               :on-click #(rf/dispatch [:fetch-detail k])}
              k]]])]]))

(defn detail-panel []
  (let [detail (rf/subscribe [:current-detail])]
     [:div.col-sm-offset-4.detail @detail]))

(defn main-panel []
  [:title "ETI"]
    [:div [nav-bar] [side-bar] [detail-panel]])

(defn top-panel
  []
  (let [ready?  (rf/subscribe [:initialised?])]
    (if-not @ready?
      [:div "... Initialising ..."]
      [main-panel])))
