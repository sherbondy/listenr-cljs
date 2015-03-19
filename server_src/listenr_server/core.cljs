(ns listenr-server.core
  (:require
   [cljs.nodejs :as nodejs]
   [figwheel.client :as fw]))

(nodejs/enable-util-print!)

(defonce express (nodejs/require "express"))
(defonce serve-static (nodejs/require "serve-static"))
(defonce http (nodejs/require "http"))

;; app gets redefined on reload
(def app (express))

;; routes get redefined on each reload
(. app (get "/hello"
            (fn [req res] (. res (send "Hello world")))))

(. app (use (serve-static "resources/public" #js {:index "index.html"})))

(def -main
  (fn []
    ;; This is the secret sauce. you want to capture a reference to
    ;; the app function (don't use it directly) this allows it to be redefined on each reload
    ;; this allows you to change routes and have them hot loaded as you
    ;; code.
    (doto (.createServer http #(app %1 %2))
      (.listen 8000))))

(set! *main-cli-fn* -main)

(fw/start { })
