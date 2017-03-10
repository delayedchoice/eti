(ns eti.server
  (:import [java.net URI] )
  (:require [eti.handler :refer [handler]]
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
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defn -main [& args]
   (let [port (Integer/parseInt (or (env :port) "3000"))]
     (run-jetty (logger/wrap-with-logger handler) {:port port :join? false})))




