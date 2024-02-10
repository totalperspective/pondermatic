(ns user
  #_{:clj-kondo/ignore [:unused-namespace]}
  (:require [pondermatic.rules :as rules]
            [pondermatic.db :as db]
            [pondermatic.portal.client :as portal]
            [pondermatic.rules.production :as prod]
            [hyperfiddle.rcf :refer [tests]]))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
;; (defonce _portal (portal/start :vs-code))

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
