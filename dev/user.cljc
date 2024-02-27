(ns user
  #_{:clj-kondo/ignore [:unused-namespace]}
  (:require [pondermatic.portal.server :as p]
            [hyperfiddle.rcf :refer [tests]]
            #?(:cljs
               [cljs.repl :as repl]
               :default
               [clojure.repl :as repl])))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defonce _portal (p/-main :vs-code))

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
