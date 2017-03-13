(ns eti.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :proxy-data
 (fn [db]
   (:proxy-data db)))

(re-frame/reg-sub        ;; we can check if there is data
  :initialised?          ;; usage (subscribe [:initialised?])
  (fn  [db _]
    (not (empty? (:proxy-data db)))))

