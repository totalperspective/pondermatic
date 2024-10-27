(ns example.callback
  (:require [pondermatic.core :as p]
            [pondermatic.shell :refer [|>]]
            [pondermatic.flow :as flow]
            [pondermatic.macros :refer [|-><]]))

(def engine (p/->engine "callback" :reset-db? true))

(def <>t (p/t>< engine))

(|->< <>t
      (flow/drain-using (flow/tapper (fn [t]
                                       (println "t:" t)))))

(|> engine {:->db (p/dataset [{:id :bob
                               :player/name "Bob"
                               :player/score 99}])
            :cb (fn [result]
                  (println "result:" result))})
