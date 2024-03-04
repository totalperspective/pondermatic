(ns example.entities
  (:require [pondermatic.core :as p]
            [pondermatic.shell :refer [|>]]
            [hyperfiddle.rcf :refer [tests %] :as rcf]
            [pondermatic.flow :as flow]
            [portal.console :as log]
            #?(:clj [pondermatic.macros :refer [|-><]]))
  #?(:cljs
     (:require-macros [pondermatic.macros :refer [|-><]])))

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

(tests
 (def tap (flow/tapper (fn [x]
                         (log/info {::q x})
                         (rcf/tap x))))
 (def <>q (p/q>< engine '[:find ?f ?s ?c
                          :where
                          [?id :data/fruit ?f]
                          [?id :data/size ?s]
                          [?id :data/color ?c]]))

 (|->< <>q (flow/drain-using ::q tap))

 (-> engine
     (|> {:->db rules})
     (|> {:->db data}))
;;  % := []
 % := [["Apple" "Large" "Red"]]
 (p/stop engine))
