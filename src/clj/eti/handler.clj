(ns eti.handler
  (:require [liberator.core :refer [resource defresource]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [compojure.route :refer (resources)]
            [ring.adapter.jetty]
            [ring.middleware.logger :as logger]
            [compojure.core :refer [rfn GET defroutes ANY] ]
            [clojure.java.io :as io]
            [liberator.dev :refer [wrap-trace]]
            [clojure.tools.logging :as log]
            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [org.httpkit.client :as http]
            [konserve.filestore :refer [new-fs-store]]
            [konserve.core :as k]
            [clojure.core.async :as async :refer [<!!]]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]))

(def store (<!! (new-fs-store "resources/store")))

(defn get-body-from-stream [resp]
  (let [body (resp :body)
        len (-> resp :headers :content-length Integer/parseInt)
        _ (log/debug (str "length: " len))]
    (with-open [rdr body]
     (let [buf (byte-array len)]
       (.read rdr buf)
         (String. buf) ))))

(defn get-body [resp]
  (let [source (resp :source)]
    (if (= source :cache)
        (resp :body)
        (get-body-from-stream resp))))

(defn build-path-and-query-string [req]
  (let [q (:query-string req)
        q-string (if (> (count q) 0)
                     (str "?" q)
                     "") ]
    (str (:uri req) q-string)))

(defn build-url [proxied-host proxied-port req]
  (let [q (:query-string req)
        q-string (if (> 0 (count q))
                     (str "?" q)
                     "") ]
    (str "http://" proxied-host ":" proxied-port (build-path-and-query-string req))))

(defn load-from-cache [req]
  (let [hit (<!! (k/get-in store [(build-path-and-query-string req)]))]
    (if hit
      (merge {:source :cache} hit)
      nil)))


(defroutes proxy-route
  (rfn req
       (let [
             _ (log/debug (str "incoming request: " req))
             out-req {:method (:request-method req)
                      :url (build-url "localhost" "3000" req)
                      ;:headers (dissoc (:headers req) "host" "content-length")
                      ;:body (if-let [len (get-in req [:headers "content-length"])]
                      ;        (slurp-binary (:body req) (Integer/parseInt len)))
                      :follow-redirects true
                      :throw-exceptions false
                      :as :stream}
             ;_ (log/debug (str "out-req: " out-req))
             response (some identity (lazy-seq [(load-from-cache req)
                                                @(http/request out-req)]))

             _ (log/debug (str "response: " response))
             _ (log/debug "printed response")
             ]
         (let [body (get-body response)
               response (assoc response :body body)
               _ (log/debug "dbresponse: " (str (<!! (k/assoc-in store [(build-path-and-query-string req)] response ) )) )]
           body))))

(def dev-handler (-> #'proxy-route
                     wrap-reload
                     #_wrap-multipart-params
                     #_logger/wrap-with-logger
                     #_(wrap-trace :header :ui)))

(def handler (-> proxy-route
                wrap-multipart-params
                ))
