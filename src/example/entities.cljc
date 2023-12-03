(ns example.entities
  (:require [pondermatic.core :as p]))

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

(p/|> engine {:->db rules})
(p/|> engine {:->db data})
