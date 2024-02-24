(ns example.entities
  (:require [pondermatic.core :as p]
            [pondermatic.shell :refer [|>]]))

(def data
  (p/dataset
   [{:id "apple"
     :fruit "Apple"
     :size "Large"
     :color "Red"}]))

(def rules
  (p/ruleset
   [{:id ::fetch-apple
     :rule/when '{":db/ident" "apple"
                  & ?e}
     :rule/then '{:local/id :apple
                  & ?e}}]))

(def engine (p/->engine "fruit" :reset-db? true))

(-> engine
    (|> {:->db rules})
    (|> {:->db data})
    p/stop)
