(ns pondermatic.portal.server
  (:require #?(:browser [portal.web :as p]
               :default [portal.api :as p])
            [pondermatic.rules.production :as prp]))

(defn compile-pattern [& patterns]
  (mapv (fn [pattern]
          {:what (prp/compile-what pattern)
           :when (prp/compile-when pattern {})})
        patterns))

(p/register! #'compile-pattern)

(def port 5678)

(defn -main [& _]
  (let [p (p/open {:port port})]
    (prn p)))
