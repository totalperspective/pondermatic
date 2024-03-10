(ns user
  #_{:clj-kondo/ignore [:unused-namespace]}
  (:require #?(:cljs/browser [pondermatic.portal.client :as p]
               :default [pondermatic.portal.server :as p])
            [hyperfiddle.rcf :refer [tests]]
            #?(:clj [clj-async-profiler.core :as prof])
            #?(:cljs
               [cljs.repl :as repl]
               :default
               [clojure.repl :as repl])))

#?(:cljs
   (enable-console-print!))

#?(:clj (prof/serve-ui 7778))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defonce _portal #?(:cljs/browser
                    (p/start nil)
                    :default
                    (p/-main :vs-code)))

(hyperfiddle.rcf/enable!)

; prevent test execution during cljs hot code reload
#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ^:dev/before-load stop []
  (hyperfiddle.rcf/enable! false))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ^:dev/after-load start []
  (hyperfiddle.rcf/enable!))

(defn ^:dev/after-load -main
  [& _])

(tests
 :enabled := :enabled)
