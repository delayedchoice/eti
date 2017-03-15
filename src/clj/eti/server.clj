(ns eti.server
  (:require [eti.handler :refer [proxy-handler web-app-handler]]
            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defn -main [& args]
   (let [proxy-port (Integer/parseInt (or (env :proxy-port) "3100"))
         web-app-port (Integer/parseInt (or (env :web-app-port) "3101"))]
     (run-jetty proxy-handler {:port proxy-port
                               :join? false})
     (run-jetty web-app-handler {:port web-app-port
                                 :join? false})))




