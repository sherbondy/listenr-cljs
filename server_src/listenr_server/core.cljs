(ns listenr-server.core
  (:require
   [cljs.nodejs :as nodejs]
   [figwheel.client :as fw]))

(nodejs/enable-util-print!)

(defonce express (nodejs/require "express"))
(defonce session (nodejs/require "express-session"))
(defonce serve-static (nodejs/require "serve-static"))
(defonce http (nodejs/require "http"))
(defonce process (nodejs/require "process"))
(defonce passport (nodejs/require "passport"))
(defonce passport-tumblr (nodejs/require "passport-tumblr"))
(defonce TumblrStrategy (.. passport-tumblr -Strategy))
(defonce request (nodejs/require "request"))

(def oauth-key (.. process -env -TUMBLR_OAUTH_KEY))
(def oauth-secret (.. process -env -TUMBLR_OAUTH_SECRET))

(defn request-oauth-map [token token_secret]
  #js {:consumer_key oauth-key
       :consumer_secret oauth-secret
       :token token
       :token_secret token_secret})

;; eventually replace with something like redis...
;; don't really care for persistence, so in-memory store is just fine.
(defonce users (atom {}))

(. passport
   (use
    (TumblrStrategy.
     #js {:consumerKey oauth-key
          :consumerSecret oauth-secret
          :callbackURL "http://ethanis.dyndns.org:8000/auth/callback"}
     (fn [token token-secret profile done]
       (swap! users assoc (.-username profile)
              {:token token
               :token_secret token-secret})
       (done nil profile)))))

(. passport
   (serializeUser
    (fn [user done]
      (done nil user))))

(. passport
   (deserializeUser
    (fn [user done]
      (done nil user))))

(defn ensure-auth [req res next]
  (if (. req (isAuthenticated))
    (next)
    (.redirect res "/auth")))
       
;; app gets redefined on reload
(def app (express))

(. app (use (serve-static "resources/public" #js {:index "index.html"})))
(. app (use (session #js {:secret "keyboard cat"})))
(. app (use (. passport (initialize))))
(. app (use (. passport (session))))

;; routes get redefined on each reload
(. app (get "/hello"
            (fn [req res] (. res (send "Hello world")))))

(. app (get "/login"
            (fn [req res] (. res (send "Login page...")))))

(. app (get "/logout"
            (fn [req res]
              (.logout req)
              (.redirect res "/"))))

(. app (get "/account" ensure-auth
            (fn [req res]
              (. res (send (.-user req))))))

(. app (get "/auth"
            (. passport (authenticate "tumblr"))
            (fn [req res] )))

(. app (get "/auth/callback"
            (. passport
               (authenticate "tumblr" #js {:failureRedirect "/login"}))
            (fn [req res] (.redirect res "/"))))

(. app (get "/logout"
            (. passport (authenticate "tumblr"))
            (fn [req res] )))

(def tumblr-base-url "http://api.tumblr.com/v2/")

(. app
   (get "/likes" ensure-auth
        (fn [req res]
          (when-let [{:keys [token token_secret]}
                     (get @users (.. req -user -username))]
            (let [oauth (request-oauth-map token token_secret)]
              (.. request
                  (get #js {:url (str tumblr-base-url "user/likes")
                            :oauth oauth
                            :json true}
                       (fn [err response body]
                         (. res (send (.. body -response -liked_posts)))))))))))

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
