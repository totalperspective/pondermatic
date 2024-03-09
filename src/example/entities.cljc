(ns example.entities
  (:require [pondermatic.core :as p]
            [hyperfiddle.rcf :refer [tests %] :as rcf]
            [pondermatic.flow :as flow]
            [portal.console :as log]
            [pondermatic.pool :as pool]
            #?(:clj [pondermatic.macros :refer [|-><]]))
  #?(:cljs
     (:require-macros [pondermatic.macros :refer [|-><]])))

(defonce pool (-> {}
                  (pool/contructor :engine p/->engine p/clone>)
                  pool/->pool))

(def |> (partial pool/to-agent! pool))

(defn ->engine [& args]
  (apply pool/add-agent! pool :engine args))

(def with-agent< (partial pool/with-agent< pool))

(def q>< (fn [id & args]
           ((with-agent< p/q><) id args)))

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

(def engine (->engine "fruit" :reset-db? true))

(tests
 (def tap (flow/tapper (fn [x]
                         (log/info {::q x})
                         (rcf/tap x))))
 (def <>q (q>< engine '[:find ?f ?s ?c
                        :where
                        [?id :data/fruit ?f]
                        [?id :data/size ?s]
                        [?id :data/color ?c]]))

 (|->< <>q (flow/drain-using ::q tap))

 (-> engine
     (|> {:->db rules})
     (|> {:->db data}))
 % := []
 % := [["Apple" "Large" "Red"]]
 (pool/remove-agent! pool engine))
