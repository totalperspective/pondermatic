(ns example.predicates
  (:require [pondermatic.core :as p]
            [pondermatic.shell :refer [|>]]))

(def data
  (p/dataset
   [{:id :bob
     :player/name "Bob"
     :player/score 99}
    {:id :alice
     :player/name "Alice"
     :player/score 200}]))

(def rules
  (p/ruleset
   '[{:id ::100-club
      :rule/when #{{:id ?id
                    :player/name ?name
                    :player/score ?score}
                   (> ?score 100)}
      :rule/then {:id ?id
                  :club/hundred? true}}]))

(def engine (p/->engine "player" :reset-db? true))

(-> engine
    (|> {:->db rules})
    (|> {:->db data})
    p/stop)
