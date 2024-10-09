(ns pondermatic.portal.server
  (:require #?(:cljs/browser [portal.web :as p]
               :default [portal.api :as p])
            [pondermatic.portal.client :as pc]
            [pondermatic.rules.production :as prp]
            #?(:cljs [pondermatic.portal.utils :as utils])
            #?(:cljs [pondermatic.portal.source-map :as source-map])
            #?(:cljs [kitchen-async.promise :as pa])
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

#?(:cljs
   (do
     (defn map-stack [stack-trace]
       (pa/let [result (source-map/apply-source-maps stack-trace)]
         (utils/text result)))
     (p/register! #'map-stack)))

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
