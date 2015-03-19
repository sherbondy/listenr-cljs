(defproject listenr "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3126"]
                 [figwheel "0.2.5"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]

  :plugins [[lein-cljsbuild "1.0.4"]
            [lein-figwheel "0.2.5"]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "out"]
  
  :cljsbuild
  {:builds
   [
    {:id "server"
     :source-paths ["server_src"]
     :compiler {:output-to "server.js"
                :output-dir "out"
                :main listenr-server.core
                :target :nodejs
                :optimizations :none
                :source-map true
                :source-map-timestamp true
                :cache-analysis true}}
    {:id "dev"
     :source-paths ["src" "dev_src"]
     :compiler {:output-to "resources/public/js/compiled/listenr.js"
                :output-dir "resources/public/js/compiled/out"
                :optimizations :none
                :main listenr.dev
                :asset-path "js/compiled/out"
                :source-map true
                :source-map-timestamp true
                :cache-analysis true }}
    {:id "min"
     :source-paths ["src"]
     :compiler {:output-to "resources/public/js/compiled/listenr.js"
                :main listenr.core                         
                :optimizations :advanced
                :pretty-print false}}]})
