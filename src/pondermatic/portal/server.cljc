(ns pondermatic.portal.server
  (:require #?(:browser [portal.web :as p]
               :default [portal.api :as p])
            [pondermatic.portal.client :as pc]
            [pondermatic.rules.production :as prp]
            [portal.console :as log]))

(defn compile-pattern [& patterns]
  (mapv (fn [pattern]
          {:what (prp/compile-what pattern)
           :when (prp/compile-when pattern {})})
        patterns))

(p/register! #'compile-pattern)

(def port 5678)

(def submit (pc/submitter p/submit))

(defn -main [& [launcher]]
  (if (keyword? launcher)
    (do
      (p/open {:launcher launcher})
      (add-tap #'submit)
      (log/info launcher))
    (do
      (let [p (p/open {:port port})]
        (prn p)))))
