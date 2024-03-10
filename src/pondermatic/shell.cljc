(ns pondermatic.shell
  (:require [missionary.core :as m]
            [pondermatic.flow :as f]
            [portal.console :as log]))

(def done ::done)

(defn actor
  ([init]
   (actor ::>return init))
  ([prefix init]
   (let [prefix (keyword (or (namespace prefix) (name prefix)) ">return")
         self (m/mbx)
         >actor (m/ap
                 (loop [process init]
                   (let [cmd (m/? self)
                         next> (process cmd)
                         session (m/? (next>))
                         next (next> nil session)]
                     (if (not= done cmd)
                       (m/amb session
                              (recur next))
                       session))))
         >return (->> >actor
                      (m/eduction (remove nil?))
                      m/stream)]
     (f/drain >return prefix)
     {::send self
      ::receive >return})))

(defn engine
  [process session]
  (let [engine (partial engine process)]
    (fn processor
      ([] (if (fn? session)
            session
            (m/sp session)))
      ([_ session] (engine session))
      ([cmd]
       (if-let [rdv (get cmd ::rdv)]
         (do (f/return rdv session)
             (engine session))
         (try
           (-> session
               (process cmd)
               engine)
           (catch #?(:cljs js/Error :default Exception) e
             (log/error e)
             (engine session))))))))

(defn |> [{:keys [::send] :as a} msg]
  (if send
    (do
      (send msg)
      a)
    (throw (ex-info "Not a valid actor" {:actor a}))))

(defn stop [engine]
  (|> engine done))

(defn |< [{:keys [::receive] :as a} create-flow & {:keys [signal?] :or {signal? false}}]
  (if receive
    (create-flow (if signal?
                   (m/signal receive)
                   receive))
    (m/ap
     (m/amb nil)
     (throw (ex-info "Not a valid actor" {:actor a})))))

(defn >->< [flow]
  (->> flow
       (m/eduction (take 1))
       (m/reduce f/latest)))

(defn flow [{:keys [::receive]}]
  receive)

(defn |<->< [a flow & {:keys [signal?] :or {signal? false}}]
  (>->< (|< a flow :signal? signal?)))

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
   (f/drain (|< actor (|<= (map (partial reset! atom))))
            ::->atom)
   atom))
