(ns eti.events
    (:require [re-frame.core :as re-frame]
							[ajax.core :as ajax]
      			  [day8.re-frame.http-fx]
              [eti.db :as db]))

(re-frame/reg-event-db
   :initialise-db
   (fn [db _]
     (assoc db :proxy-data ["url" "data"])))

(re-frame/reg-event-db
  :bad-response
  (fn
    [db [_ response]]
    (let [_ (prn "response: " response)]
      (-> db
         (assoc :loading? false)
         (assoc :intialized? true)
         (assoc :proxy-data (str "BadResponse: " (js->clj response)))))))

(re-frame/reg-event-db
  :process-response
  (fn
    [db [_ response]]
    (let [_ (prn "response: " response)
          data (:data (js->clj response))
          _ (prn "data: " data)]
      (doseq [d data]
        (re-frame/dispatch [:fetch-detail-data d]))
      (-> db
         (assoc :loading? false)
         (assoc :intialized? true)
         (assoc :proxy-data
                (into {} (for [d data] [d nil])))))))

(re-frame/reg-event-db
  :process-detail-response
  (fn
    [db [_ response]]
    (let [_ (prn "response: " response)
          data (js->clj response)
          e    (:entry data)
          url  (:link data)]
      (-> db
          (assoc-in [:proxy-data url] e)))))

(re-frame/reg-event-fx
  :fetch-detail-data
  (fn
    [{db :db} [_ url]]
    (let [_ (prn "url: " url)]
      {:http-xhrio {:method          :get
                   :uri             url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:process-detail-response]
                   :on-failure      [:bad-response]}
      :db  db})))

(re-frame/reg-event-fx
  :collect-data
  (fn
    [{db :db} _]
    {:http-xhrio {:method          :get
                  :uri             "http://localhost:3000/eti"
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:process-response]
                  :on-failure      [:bad-response]}
     :db  (assoc db :loading? true)}))
