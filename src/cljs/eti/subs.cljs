(ns eti.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as rf]))

(rf/reg-sub
 :proxy-data
 (fn [db]
   (:proxy-data db)))

(rf/reg-sub
 :error-message
 (fn [db]
   (:error-message db)))

(rf/reg-sub
 :current-detail
 (fn [db]
   (:current-detail db)))

(rf/reg-sub
 :selected-url
 (fn [db]
   (:selected-url db)))

(rf/reg-sub
  :initialised?
  (fn  [db _]
    (not (empty? (:proxy-data db)))))

