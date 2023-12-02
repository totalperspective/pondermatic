(ns pondermatic.shell
  (:require [missionary.core :as m]
            [pondermatic.flow :as f]))

(defn return [emit result]
  (let [run (m/sp (m/? (emit result)))]
    (run identity f/crash)
    result))

(def done ::done)

(defn actor [init]
  (let [self (m/mbx)
        >return (m/stream
                 (m/eduction
                  (remove nil?)
                  (m/ap
                   (loop [process init]
                     (let [cmd (m/? self)]
                       (if (not= done cmd)
                         (let [emit (m/rdv)
                               next (process (partial return emit) cmd)]
                           (m/amb
                            (m/? emit)
                            (recur next)))
                         (do
                           (process identity done)
                           (println done)
                           nil)))))))]
    (f/drain >return)
    {::send self
     ::receive >return}))

(defn engine
  [process session]
  (let [engine (partial engine process)]
    (fn processor [ret cmd]
      (if-let [rdv (get cmd ::rdv)]
        (do (return rdv session)
            (-> session ret engine))
        (-> session
            (process cmd)
            ret
            engine)))))

(defn |> [{:keys [::send] :as a} msg]
  (if send
    (do
      (send msg)
      a)
    (throw (ex-info "Not a valid actor" {:actor a}))))

(defn |< [{:keys [::receive] :as a} create-flow & {:keys [signal?] :or {signal? false}}]
  (if receive
    (create-flow (if signal?
                   (m/signal receive)
                   receive))
    (m/ap
     (m/amb nil)
     (throw (ex-info "Not a valid actor" {:actor a})))))

(defn >< [flow]
  (->> flow
       (m/eduction (take 1))
       (m/reduce f/latest)))

(defn flow [{:keys [::receive]}]
  receive)

(defn |>< [a flow & {:keys [signal?] :or {signal? false}}]
  (>< (|< a flow :signal? signal?)))

(defn |<= [& xf*]
  (fn [& arg*]
    (->> arg*
         (into (vec xf*))
         (apply m/eduction))))

(defn |!> [{:keys [::send] :as a} fn]
  (if send
    (let [return (m/rdv)]
      (send {::rdv return})
      (m/sp (fn (m/? return))))
    (throw (ex-info "Not a valid actor" {:actor a}))))

(defn ->atom
  ([actor]
   (->atom actor (atom nil)))
  ([actor atom]
   (f/drain (|< actor (|<= (map (partial reset! atom)))))
   atom))
