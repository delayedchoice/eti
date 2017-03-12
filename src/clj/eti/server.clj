(ns eti.server
  (:import [java.net URI] )
  (:require [eti.handler :as util]
            [config.core :refer [env]]
            [ring.middleware.logger :as logger]
            [compojure.core :refer [defroutes rfn] ]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [clojure.tools.logging :as log]
            [ring.middleware.logger :as logger]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [clojure.string          :refer [join split]]
            [org.httpkit.client :as http]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [clojure.core.async :as async :refer [<!!]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defroutes proxy-route
  (rfn req
    (let [out-req {:method (:request-method req)
                   :url (util/build-proxy-url "localhost" "3000" req)
                   :follow-redirects true
                   :throw-exceptions false
                   :as :stream }
         response (some identity (lazy-seq [(util/load-from-cache req)
                                            @(http/request out-req)]))
        _ (log/debug "response0: " response)
         ;len (-> response :headers :content-length Integer/parseInt)
				 body (util/body-as-string response )
         _ (log/debug "response1: " response)
         response (assoc response :body body)
         cache-response (util/store (util/build-path-and-query-string req) response)
         _ (log/debug "response: " response)
         _ (log/debug "cache-response: " cache-response)]
       body)))

(def handler
  (-> #'proxy-route
      wrap-reload
      wrap-keyword-params
      logger/wrap-with-logger
      ))

(defn -main [& args]
   (let [port (Integer/parseInt (or (env :port) "3000"))]
     (run-jetty handler {:port port :join? false})))




