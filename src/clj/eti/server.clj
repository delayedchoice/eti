(ns eti.server
  (:require [eti.handler :refer [dev-handler]]
            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defn -main [& args]
   (let [port (Integer/parseInt (or (env :port) "3000"))]
     (run-jetty dev-handler {
                            ; :response-header-size 819200
                            ; :output-buffer-size 99999999
                            ; :request-header-size 81920
                             :port port
                             :join? false})))




