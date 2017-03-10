(ns eti.handler
  (:require [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.adapter.jetty]
            [ring.middleware.logger :as logger]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [compojure.core :refer [rfn GET POST defroutes ANY] ]
            [compojure.route :refer [resources]]
            ;[clojure.java.io :as io]
            [liberator.core :refer [resource defresource]]
            [liberator.dev :refer [wrap-trace]]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [org.httpkit.client :as http]
            [konserve.filestore :as fs]
            [konserve.core :as k]
            [clojure.data.json :as json]
            [clojure.core.async :as async :refer [<!!]]
            ))

(def store (<!! (fs/new-fs-store "resources/store")))

(defn get-body-from-stream [resp len]
  (let [body (resp :body) ]
    (with-open [rdr body]
     (let [buf (byte-array len)]
       (.read rdr buf)
         (String. buf)))))

(defn body-as-string [ctx]
  (if-let [body (get-in ctx [:body])]
    (let [rv (condp instance? body
              java.lang.String body
              (slurp (io/reader body)))
          _ (log/debug "extracred-body: " rv)]
      rv)))

(defn get-body [resp len]
  (let [source (resp :source)]
    (if (= source :cache)
        (resp :body)
        (get-body-from-stream resp len))))

(defn build-path-and-query-string [req]
  (let [q (:query-string req)
        q-string (if (> (count q) 0)
                     (str "?" q)
                     "") ]
    (str (:uri req) q-string)))

(defn build-url [proxied-host proxied-port req]
  (str "http://"
       proxied-host
       ":"
       proxied-port
       (build-path-and-query-string req)))

(defn load-from-cache [req]
  (let [hit (<!! (k/get-in store [(build-path-and-query-string req)]))]
    (if hit
       hit
        nil)))

(defroutes proxy-route
  (GET "/eti" []
    (resource :available-media-types ["application/json"]
              :handle-ok
                (fn [ctx]
                  (let [kys (<!! (fs/list-keys store))
                        entries (for [ky kys] [(first ky) (<!! (k/get-in store ky))] )]
                    (into {} entries)))))
  (POST "/eti/*" []
       (resource
        :allowed-methods [:post]
        :available-media-types ["application/json"]
        :post! (fn [ctx] (let [req (:request ctx)]
                           (log/debug "req:  " req)
                           (log/debug "post-key: " (build-path-and-query-string req ))
                           (let [;len (-> req :headers (get "content-length" ) Integer/parseInt )
                                 body (json/read-str (str "{" (body-as-string req) "}") :key-fn keyword)
                                 req (assoc req :body body)
                                 _ (log/debug "body: " body)
                                 req (dissoc req :async-channel)
                                 ky  (build-path-and-query-string req)
                                 ky  (apply str (drop 4 ky))
                                 kyky (keyword ky)
                                 _ (log/debug "key: " (keyword  ky ))
                                _ (log/debug "member: " (get body kyky ) )
                                 v (<!! (k/assoc-in store [ky] (get body kyky )))]
                            (log/debug "v: " v)
                            v)))
        :new? (fn [_] true)))
  (rfn req
    (let [out-req {:method (:request-method req)
                   :url (build-url "localhost" "3000" req)
                   :follow-redirects true
                   :throw-exceptions false
                   :as :stream }
         response (some identity (lazy-seq [(load-from-cache req)
                                            @(http/request out-req)]))
        _ (log/debug "response0: " response)
         ;len (-> response :headers :content-length Integer/parseInt)
				 body (body-as-string response )
         _ (log/debug "response1: " response)
         response (assoc response :body body)
         cache-response (<!! (k/assoc-in store [(build-path-and-query-string req)] response))
         _ (log/debug "response: " response)
         _ (log/debug "cache-response: " cache-response)]
       body)))

(def dev-handler
  (-> #'proxy-route
      wrap-reload
      wrap-keyword-params
      logger/wrap-with-logger))

(def handler
  (-> proxy-route
      wrap-multipart-params))
