(ns eti.events
    (:require [re-frame.core :as re-frame]
							[ajax.core :as ajax]
      			  [day8.re-frame.http-fx]
              [eti.db :as db]
              [cljs.pprint :as pp]
              ))

(re-frame/reg-event-db
  :bad-response
  (fn
    [db [_ response]]
      (-> db
          (assoc :loading? false)
          (assoc :intialized? true)
          (assoc :error-message (str "Bad Response: " (js->clj response))))))

(re-frame/reg-event-db
  :process-response
  (fn
    [db [_ response]]
    (let [data (:data (js->clj response))]
      (-> db
         (assoc :loading? false)
         (assoc :intialized? true)
         (assoc :proxy-data (into {} (for [d data] [d "detail-entry list-group-item"])))))))

(re-frame/reg-event-db
  :process-detail-response
  (fn
    [db [_ response]]
    (let [data (js->clj response)
          e    (:entry data)]
      (-> db
          (assoc-in [:original-detail] e)
          (assoc-in [:current-detail] e)))))

(re-frame/reg-event-db
  :process-put-response
  (fn [db [_ response]]
      (-> db
          (assoc-in [:original-detail] (db :current-detail))
          (assoc-in [:error-message] nil))))

(re-frame/reg-event-fx
  :fetch-detail
  (fn [{db :db} [_ url]]
      {:http-xhrio {:method          :get
                    :uri             url
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:process-detail-response]
                    :on-failure      [:bad-response]}
       :db  (-> (if (:selected-url db)
                    (assoc-in db [:proxy-data (:selected-url db)] "detail-entry list-group-item")
                    db)
                (assoc-in [:selected-url] url)
                (assoc-in [:proxy-data url] "active detail-entry list-group-item" ))}))

(re-frame/reg-event-db
  :content-edited
  (fn
    [db [_ content]]
      (let [_ (prn "content: " content)]
        (assoc db :current-detail content))))

(re-frame/reg-event-db
  :cursor-moved
  (fn
    [db [_ pos]]
        (assoc db :cursor-position pos)))

(re-frame/reg-event-fx
  :put-detail
  (fn
    [{db :db} _]
    {:http-xhrio {:method          :put
                  :uri             (:selected-url db)
                  :params          (:current-detail db)
                  :timeout         5000
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:process-put-response]
                  :on-failure      [:bad-response]}
     :db  (assoc db :loading? true)}))

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
