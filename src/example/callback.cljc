(ns example.callback
  (:require [pondermatic.core :as p]
            [pondermatic.shell :refer [|>]]))

(def engine (p/->engine "callback" :reset-db? true))

(|> engine {:->db (p/dataset [{:id :bob
                               :player/name "Bob"
                               :player/score 99}])
            :cb (fn [result]
                  (println "result:" result))})
