(ns eti.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as rf]))

(rf/reg-sub
 :proxy-data
 (fn [db]
   (:proxy-data db)))

(rf/reg-sub
 :sumbit-button-class
 (fn [db]
   (let [current  (db :current-detail)
         original (db :original-detail)]
     (if (= current original)
         "btn btn-primary pull-right disabled"
         "btn btn-primary pull-right"))))

(rf/reg-sub
 :content-detail-class
 (fn [db]
   (let [current  (db :current-detail)
         original (db :original-detail)]
     (if (= current original)
         "col-sm-12 detail unedited"
         "col-sm-12 detail edited"))))

(rf/reg-sub
 :error-message
 (fn [db]
   (:error-message db)))

(rf/reg-sub
 :current-detail
 (fn [db]
   (:current-detail db)))

(rf/reg-sub
 :cursor-position
 (fn [db]
   (:cursor-position db)))

(rf/reg-sub
 :selected-url
 (fn [db]
   (:selected-url db)))

(rf/reg-sub
  :initialized?
  (fn  [db _]
    (:initialized? db)))

(rf/reg-sub
  :top-class
  (fn  [db _]
    (if (:loading? db)
        "overlay"
        "" )))

