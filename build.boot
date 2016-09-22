(set-env!
  :resource-paths #{"sass" "src" "html"}
  :dependencies '[[adzerk/boot-cljs "1.7.228-1" :scope "test"]
                  [adzerk/boot-cljs-repl "0.3.3" :scope "test"]
                  [adzerk/boot-reload "0.4.12" :scope "test"]
                  [pandeiro/boot-http "0.7.1-SNAPSHOT" :scope "test"]
                  [crisptrutski/boot-cljs-test "0.2.2-SNAPSHOT" :scope "test"]
                  [org.clojure/clojure "1.9.0-alpha12"]
                  [org.clojure/clojurescript "1.9.229"]
                  [org.clojure/core.async "0.2.391"]
                  [binaryage/devtools      "0.8.2" :scope "test"]
                  [binaryage/dirac         "0.6.6" :scope "test"]
                  [powerlaces/boot-cljs-devtools "0.1.1" :scope "test"]
                  [reagent "0.6.0-rc"]
                  [com.cemerick/piggieback "0.2.1" :scope "test"]
                  [deraen/boot-sass "0.2.1" :scope "test"]
                  [weasel "0.7.0" :scope "test"]
                  [org.clojure/tools.nrepl "0.2.12" :scope "test"]])

(require
  '[adzerk.boot-cljs :refer [cljs]]
  '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
  '[adzerk.boot-reload :refer [reload]]
  '[deraen.boot-sass :refer [sass]]
  '[crisptrutski.boot-cljs-test :refer [exit! test-cljs]]
  '[pandeiro.boot-http :refer [serve]]
  '[powerlaces.boot-cljs-devtools :refer [cljs-devtools]])

(def repl-port 5600)

(deftask testing []
         (merge-env! :resource-paths #{"test"})
         identity)

(deftask auto-test []
         (comp (testing)
               (watch)
               (speak)
               (test-cljs)))

(deftask dev []
         (comp (serve :dir "static/")
               (watch)
               (speak)
               (sass :output-style :expanded)
               (reload :on-jsload 'app.core/main)
               (cljs-repl :nrepl-opts {:port repl-port})
               (cljs-devtools)
               (cljs :source-map true :optimizations :none)
               (target :dir #{"static/"})))

(deftask test []
         (comp (testing)
               (test-cljs)
               (exit!)))

(deftask build []
         (cljs :optimizations :advanced))
