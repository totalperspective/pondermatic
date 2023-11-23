(ns pondermatic.shell
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
    (f/drain >return)
    {::send self
     ::receive >return}))

(defn engine
  [process session]
  (let [engine (partial engine process)]
    (fn [return cmd]
      (-> session
          (process cmd)
          return
          engine))))

(def done ::done)

(defn ^:export |> [{:keys [::send] :as a} msg]
  (if send
    (do
      (send msg)
      a)
    (throw (ex-info "Not a valid actor" {:actor a}))))

(defn ^:export |< [{:keys [::receive] :as a} create-flow]
  (if receive
    (create-flow receive)
    (m/ap
     (m/amb nil)
     (throw (ex-info "Not a valid actor" {:actor a})))))

(defn ^:export >< [flow]
  (->> flow
       (m/eduction (take 1))
       (m/reduce f/latest)))

(defn ^:export |>< [a flow]
  (>< (|< a flow)))

(defn ^:export |<= [& xf*]
  (fn [& arg*]
    (->> arg*
         (into (vec xf*))
         (apply m/eduction))))
