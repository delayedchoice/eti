(ns eti.views
    (:require [cljs.pprint :as pp]
              [re-frame.core :as rf]))

(defn error-msg []
  (let [msg (rf/subscribe [:error-message])]
        (when @msg [:div.alert.alert-danger.col-sm-12 {:role :alert} @msg] )))

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
    (let [data @(rf/subscribe [:proxy-data])]
      [:div.col-sm-4
        [:div.nav.nav-sidebar.sidebar.list-group
        (doall
          (for [k  (keys data)]
            ^{:key k}
            [:a {:data-toggle "tooltip"
                 :data-container "body"
                 :class (get data k)
                 :title k
                 :on-click #(rf/dispatch [:fetch-detail k])} k]))]]))

(defn detail-panel []
  (let [detail (rf/subscribe [:current-detail])]
    (fn [] [:input.col-sm-offset-4.detail {:type "text"
                                           :value (with-out-str (pp/pprint @detail))
                                           :on-change #(rf/dispatch [:content-edited (cljs.reader/read-string (-> % .-target .-value))])} ] )))

(defn submit-button []
  [:button.btn.btn-primary {:on-click #(rf/dispatch [:put-detail])} ])

(defn main-panel []
  [:title "ETI"]
    [:div [nav-bar] [error-msg] [side-bar] [detail-panel] [submit-button]])

(defn top-panel
  []
  (let [ready?  (rf/subscribe [:initialised?])]
    (if-not @ready?
      [:div "... Initialising ..."]
      [main-panel])))
