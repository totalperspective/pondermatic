(ns user
  #_{:clj-kondo/ignore [:unused-namespace]}
  (:require [pondermatic.rules :as rules]
            [pondermatic.db :as db]
            [pondermatic.rules.production :as prod]
            [portal.api :as p]
            [clojure.datafy :as datafy]
            [hyperfiddle.rcf :refer [tests]]))

(def submit (comp p/submit datafy/datafy))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defonce _portal (let [p (p/open {:launcher :vs-code})]
                   (add-tap #'submit)
                   p)) ; Add portal as a tap> target

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
