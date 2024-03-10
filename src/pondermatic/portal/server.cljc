(ns pondermatic.portal.server
  (:require #?(:cljs/browser [portal.web :as p]
               :default [portal.api :as p])
            [pondermatic.portal.client :as pc]
            [pondermatic.rules.production :as prp]
            #?(:cljs [pondermatic.data :as data])
            [portal.console :as log]))

(defn compile-pattern [& patterns]
  (mapv (fn [pattern]
          {:what (prp/compile-what pattern)
           :when (prp/compile-when pattern {})})
        patterns))

(p/register! #'compile-pattern)
#?(:cljs
   (p/register! #'data/read-transit))

(def port 5678)

(def submit (pc/submitter p/submit))

(defn -main [& [launcher]]
  (if (keyword? launcher)
    (do
      (p/open {:launcher launcher})
      (add-tap #'submit)
      (log/info launcher))
    (do
      (let [p (p/open (merge {:port port}
                             (if launcher
                               {:launcher (keyword launcher)}
                               {})))]
        (prn p)))))
