(ns pondermatic.actor
  (:require [missionary.core :as m]
            [pondermatic.flow :as f]))


(defn return [emit result]
  (let [run (m/sp (m/? (emit result)))]
    (run identity f/crash)
    result))

(defn actor [init]
  (let [self (m/mbx)
        >return (m/stream
                 (m/eduction
                  (remove nil?)
                  (m/ap
                   (loop [process init]
                     (let [cmd (m/? self)]
                       (if (not= ::done cmd)
                         (let [emit (m/rdv)
                               next (process (partial return emit) cmd)]
                           (m/amb
                            (m/? emit)
                            (recur next)))
                         (do
                           (println ::done)
                           nil)))))))]
    (f/drain nil >return)
    {::send self
     ::recieve >return}))

(defn engine
  [process session]
  (let [engine (partial engine process)]
    (fn [return cmd]
      (-> session
          (process cmd)
          return
          engine))))

(def done ::done)

(defn |> [{:keys [::send] :as a} msg]
  (if send
    (do
      (send msg)
      a)
    (throw (ex-info "Not a valid actor" {:actor a}))))

(defn |< [{:keys [::recieve] :as a} create-flow]
  (if recieve
    (create-flow recieve)
    (m/ap
     (m/amb nil)
     (throw (ex-info "Not a valid actor" {:actor a})))))
