(ns eti.handler
  (:import java.net.URL)
  (:require
    ;[ring.middleware.multipart-params :refer [wrap-multipart-params]]
;            [ring.adapter.jetty]
            [ring.middleware.logger :as logger]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-body]]
            [ring.util.response :as resp]
            [compojure.core :refer [rfn GET POST defroutes ANY] ]
            [compojure.route :refer [resources]]
            [liberator.core :refer [resource defresource]]
            [liberator.dev :refer [wrap-trace]]
            [liberator.representation :refer [ring-response]]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.core.async :as async :refer [<!!]]
            [org.httpkit.client :as http]
            [konserve.filestore :as fs]
            [konserve.core :as k]
            [config.core :refer [env]]
            ))

(def cache (<!! (fs/new-fs-store "proxy-store")))

(defn store [id v]
  (<!! (k/assoc-in cache [id] v)))

(defn build-path-and-query-string [req]
  (let [q (:query-string req)
        q-string (if (> (count q) 0)
                     (str "?" q)
                     "") ]
    (str (:uri req) q-string)))

(defn get-body-as-string [req]
  (if-let [body (get-in req [:body])]
    (let [_ (log/info "raw-body: " body)
          _ (log/info "type: " (type body))
          rv (condp instance? body
                    java.lang.String body
                    (slurp (io/reader body)))
          _ (log/info "extracted-body: " rv)]
      rv)))

(defn parse-id [ctx]
  (let [_ (log/debug "ctx-for-id: " ctx)
        req (:request ctx)
        ky (build-path-and-query-string req)
        ky (apply str (drop 4 ky))]
    [false {::id ky}]))

(defn parse-json [ctx ky]
  (let [_ (log/debug "ctx3: " ctx)]
    (when (#{:put :post} (get-in ctx [:request :request-method]))
     (try
       (if-let [body (get-in (:request ctx ) [:body])]
         (let [_ (log/info "body-type: " (type body))
               data body #_(json/read-str body :key-fn keyword)]
           [false {ky data}])
         {:message "No body"})
       (catch Exception e
         (.printStackTrace e)
         {:message (format "IOException: %s" (.getMessage e))})))))

(defn check-content-type [ctx content-types]
  (if (#{:put :post} (get-in ctx [:request :request-method]))
    (or
     (some #{(get-in ctx [:request :headers "content-type"])}
           content-types)
     [false {:message "Unsupported Content-Type"}])
    true))

(defn build-proxy-url [proxied-host proxied-port req]
  (str "http://"
       proxied-host
       ":"
       proxied-port
       (build-path-and-query-string req)))

(defn build-entry-url [request id]
  (URL. (format "%s://%s:%s%s%s"
                (name (:scheme request))
                (:server-name request)
                (:server-port request)
                "/eti"
                id)))

(defn build-list-entry-url [request id]
  (URL. (format "%s://%s:%s%s/%s"
                (name (:scheme request))
                (:server-name request)
                (:server-port request)
                (:uri request)
                id)))

(defn load-from-cache [req]
  (let [hit (<!! (k/get-in cache [(build-path-and-query-string req)]))
        _ (log/info "from-cache: " hit)]
    hit))

(defresource list-resource
  :available-media-types ["application/json"]
  :allowed-methods [:get :post :delete :options]
  :known-content-type? #(check-content-type % ["application/json"])
  :malformed? #(parse-json % ::body)
  :post! #(let [id (build-path-and-query-string (:request %))]
            (<!! (k/assoc-in cache [id] (get (::body %))))
            {::id id})
  :delete! (fn [ctx]
             (let [kys (<!! (fs/list-keys cache))]
              (doseq [k kys]
                (let [_ (log/info "dissocing: " k)]
                  (<!! (k/dissoc cache (first k) ))))))
  :post-redirect? true
  :location #(build-list-entry-url (:request %) (::id %))
  :handle-ok #(ring-response {:data  (map (fn [id] (let [id (str id)
                                                          id (subs id 3 (- (count id) 2))
                                                          _ (log/info "id: " id)]
                                                      (str (build-list-entry-url (get % :request) id))))
                                           (<!! (fs/list-keys cache)) )}
                             {:headers {"Access-Control-Allow-Origin" "*"}} ))

(defresource entry-resource
  :uri-too-long? #(parse-id %)
  :allowed-methods [:get :put :delete :options]
  :known-content-type? #(check-content-type % ["application/json"])
  :new? (fn [ctx]
             (let [_ (log/info "id-for-new " (::id ctx))
                   e (<!! (k/get-in cache [(::id ctx)]))
                   _ (log/info "entity-from-cache-for-new: " e)]
                    (nil? e)))
  :exists? (fn [ctx]
             (let [_ (log/info "id-for-exists: " (::id ctx))
                   e (<!! (k/get-in cache [(::id ctx)]))
                   _ (log/info "entity-from-cache: " e)]
                    (if-not (nil? e)
                      {::entry e})))
  :existed? (fn [ctx] (let [resp (nil? (some #{(::id ctx)} (<!! (fs/list-keys cache))))
                            - (log/info "existed? " resp)]))
  :available-media-types ["application/json"]
  :handle-ok (fn [ctx] (let [_ (log/info "ok-entry: " (ctx ::entry))]
                         (ring-response {:link (str (build-entry-url (get ctx :request) (::id ctx)))
                                        :entry (ctx ::entry)}
                                       {:headers {"Access-Control-Allow-Origin" "*"}})))
  :delete! (fn [ctx] (k/dissoc cache (::id ctx)))
  :malformed? #(parse-json % ::data)
  :can-put-to-missing? false
  :put! #(let [e (second (<!! (k/assoc-in cache [(::id %)] (::data %) )))]
           (let [_ (log/info "put! entry" e)]
             (ring-response {:entry e}
                            {:headers {"Access-Control-Allow-Origin" "*"}})))
  ;:new? (fn [ctx] (nil? (some #{(::id ctx)} (<!! (fs/list-keys cache)))))
  )

(defn allow-cross-origin
  "middleware function to allow crosss origin"
  [handler]
  (fn [request]
   (let [response (handler request)]
    (-> response
        (assoc-in [:headers "Access-Control-Allow-Headers"] "Content-Type, X-Requested-With")
        (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
        (assoc-in [:headers "Access-Control-Allow-Methods"] "GET,PUT,POST,DELETE")))))

(defroutes web-app-route
  (GET "/" [] (resp/resource-response "index.html" {:root "public"}))
  (resources "/"))

(defroutes proxy-route
  (ANY "/eti/*" [] entry-resource)
  (ANY "/eti" [] list-resource)
  (rfn req
    (let [out-req {:method (:request-method req)
                   :url (build-proxy-url (or (env :proxy-target-host) "cnn.com") (or (env :proxy-target-port) "80") req)
                   :follow-redirects true
                   :throw-exceptions false
                   :as :stream }
        response (or (load-from-cache req) @(http/request out-req))
         _ (log/info "orig-response: " response)
				 body (get-body-as-string response )
         _ (log/info "body-from-response " body)
         response (assoc response :body body)
         _  (log/info "response-to-store: " response)
         cache-response (<!! (k/assoc-in cache [(build-path-and-query-string req)] response))
         _ (log/info "inserted-cache-response: " cache-response)]
       body)))

(def proxy-handler
    (-> #'proxy-route
       (wrap-json-body {:keywords? true})
       wrap-reload
       wrap-keyword-params
       logger/wrap-with-logger
       allow-cross-origin
       #_(wrap-trace :header :ui)))

(def web-app-handler
    (-> #'web-app-route))

(def handler
  (-> proxy-route
      (wrap-json-body {:keywords? true})
      wrap-keyword-params
      logger/wrap-with-logger
      wrap-multipart-params))
