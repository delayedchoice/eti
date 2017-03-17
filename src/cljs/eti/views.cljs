(ns eti.views
    (:require [cljs.pprint :as pp]
              [reagent.core :as reagent]
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

(defn detail-text-area[]
  (let [text (rf/subscribe [:current-detail])
        pos  (rf/subscribe [:cursor-position]) ]
    (reagent/create-class
     {:component-did-update
      #(.setSelectionRange (js/document.getElementById "detail-content") @pos @pos)
     :display-name "detail-text-area"
      :reagent/render
      (fn [id]
        (let [clazz (rf/subscribe [:content-detail-class])] [:textarea
          {:type "text"
           :class @clazz
           :id "detail-content"
           :value (with-out-str (pp/pprint @text))
           :on-change #(let [content (cljs.reader/read-string (-> % .-target .-value))
                             cursor-position (-> % .-target .-selectionStart)]
                         (rf/dispatch [:content-edited content])
                         (rf/dispatch [:cursor-moved cursor-position]))}]))})))


(defn submit-button []
  (let [clazz (rf/subscribe [:sumbit-button-class])]
    [:div.col-sm-12.submit-button
      [:button {:class @clazz
                :on-click #(rf/dispatch [:put-detail])} "Submit"]]) )

(defn form-area []
  [:div.col-sm-8.col-offset-sm-4
   [:div [detail-text-area] [submit-button]]])

(defn main-panel []
  [:title "ETI"]
    [:div [nav-bar] [error-msg] [:div.row [side-bar] [form-area]] ])

(defn top-panel
  []
  (let [ready?  (rf/subscribe [:initialised?])]
    (if-not @ready?
      [:div "... Initialising ..."]
      [main-panel])))
