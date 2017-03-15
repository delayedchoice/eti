(ns eti.events
    (:require [re-frame.core :as re-frame]
							[ajax.core :as ajax]
      			  [day8.re-frame.http-fx]
              [eti.db :as db]))

(re-frame/reg-event-db
  :bad-response
  (fn
    [db [_ response]]
      (-> db
          (assoc :loading? false)
          (assoc :intialized? true)
          (assoc :proxy-data (str "Bad Response: " (js->clj response))))))

(re-frame/reg-event-db
  :process-response
  (fn
    [db [_ response]]
    (let [data (:data (js->clj response))]
      (-> db
         (assoc :loading? false)
         (assoc :intialized? true)
         (assoc :proxy-data (into {} (for [d data] [d nil])))))))

(re-frame/reg-event-db
  :process-detail-response
  (fn
    [db [_ response]]
    (let [data (js->clj response)
          e    (:entry data)]
      (-> db
          (assoc-in [:current-detail] (with-out-str (cljs.pprint/pprint e)))))))

(re-frame/reg-event-fx
  :fetch-detail
  (fn [{db :db} [_ url]]
      {:http-xhrio {:method          :get
                    :uri             url
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:process-detail-response]
                    :on-failure      [:bad-response]}
       :db  db}))

(re-frame/reg-event-fx
  :collect-data
  (fn
    [{db :db} _]
    {:http-xhrio {:method          :get
                  :uri             "http://localhost:3100/eti"
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:process-response]
                  :on-failure      [:bad-response]}
     :db  (assoc db :loading? true)}))
