(ns gennai2es.core
  (:use compojure.core
        clojure.pprint)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.json :as middleware]
            [org.httpkit.server :as https]
            [org.httpkit.client :as httpc]
            [clojure.data.json :as json]))
;
; http://udayv.com/clojure/2014/08/19/json-web-services-with-clojure/
;

(def counter (atom 0N))
(defonce server (atom nil))

(defn multiSend [x]
  (let [futures
        (map
         #(let [strJson (json/write-str %)]
            (httpc/post "http://localhost:9200/samoa/proxytest"
                       {:headers {"Content-type" "application/json"}
                        :body strJson}))
         x)]
    (doseq [resp futures]
      ;; wait for server response synchronously
      (println (-> @resp :opts :url) (-> @resp :opts :body) " status: " (:status @resp))
      (swap! counter inc)))
  @counter)

(defroutes app-routes
  (POST "/" request
        (let [objBody (:body request)
              intCount (multiSend objBody)]
          (reset! counter 0N)
          {:status 200
           :body (str "OK: " intCount)}))
  (route/resources "/")
  (route/not-found "Not Found"))

; - - - -
(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(def app
  (-> (handler/site app-routes)
      (middleware/wrap-json-body {:keywords? true})
      middleware/wrap-json-response))

(defn -main [&args]
  ;; The #' is useful when you want to hot-reload code
  ;; You may want to take a look: https://github.com/clojure/tools.namespace
  ;; and http://http-kit.org/migration.html#reload
  (reset! server (https/run-server app {:port 8080})))
