(ns example.callback
  (:require [pondermatic.core :as p]
            [pondermatic.shell :refer [|>] :as sh]
            [pondermatic.flow :as flow]
            [pondermatic.macros :refer [|-><]]
            [portal.console :as log]))

(def engine (p/->engine "callback" :reset-db? true))

(def <>t (p/t>< engine))

(log/info {:quiescent? (sh/quiescent? engine)})

(|->< <>t
      (flow/drain-using (flow/tapper (fn [t]
                                       (log/info {::tx t})))))

(|> engine {:->db (p/dataset [{:id :bob
                               :player/name "Bob"
                               :player/score 99}])
            :cb (fn [result]
                  (log/info result))})

(log/info {:quiescent? (sh/quiescent? engine)})

(|> engine {:noop true
            :cb (fn [result]
                  (log/info result))})

(|> engine {:!>db :noop
            :cb (fn [result]
                  (log/info {::db result}))})

(|> engine {:->rules '(noop)
            :cb (fn [result]
                  (log/info result))})

(|> engine {:noop true
            :cb (fn [result]
                  (log/info result))})

(log/info {:quiescent? (sh/quiescent? engine)})
