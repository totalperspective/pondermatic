(ns pondermatic.shell
  (:require [missionary.core :as m]
            [pondermatic.flow :as f]
            [portal.console :as log]
            [clojure.walk :as walk]))

(def done ::done)

(defn cb [cmd]
  (let [cmd-meta (try (meta cmd)
                      (catch #?(:clj Exception :cljs js/Error) _e nil))]
    (get cmd-meta ::cb identity)))

(defn elide-fns [x]
  (walk/postwalk (fn elide-fns [x]
                   (if (fn? x)
                     ::fn
                     x))
                 x))

(defn equal? [a b]
  (= (elide-fns a) (elide-fns b)))

(defn actor
  ([init]
   (actor ::>return init))
  ([prefix init]
   (let [!quiescent? (atom true)
         prefix (keyword (or (namespace prefix) (name prefix)) ">return")
         self (m/mbx)
         >actor (m/ap
                 (loop [process init last-session nil]
                   (let [cmd (m/? self)
                         cb (cb cmd)]
                     (log/trace {:prefix prefix :cmd cmd :cb cb})
                     (reset! !quiescent? false)
                     (let [next> (process cmd)
                           session (m/? (next>))
                           next (next> nil session)
                           session-cb (::cb (meta session))
                           {:keys [::safe-keys]} (meta session)
                           safe-session (if (map? session)
                                          (select-keys session safe-keys)
                                          {:session session})]
                       (reset! !quiescent? (equal? session last-session))
                       (let [cb-msg {:prefix prefix
                                     :quiescent? @!quiescent?}
                             cb-msg (merge cb-msg safe-session)]
                         (when cb
                           (cb cb-msg))
                         (when session-cb
                           (session-cb cb-msg)))
                       (if (not= done cmd)
                         (m/amb  session
                                 (recur next session))
                         session)))))
         >return (->> >actor
                      (m/eduction (remove nil?))
                      m/stream)]
     (f/drain >return prefix)
     {::prefix prefix
      ::send self
      ::receive >return
      ::!quiescent? !quiescent?})))

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
             #?(:cljs (js/console.error "Engine process failed" e))
             (log/error (ex-info "Engine process failed" {::cmd cmd} e))
             (engine session))))))))

(defn |> [{:keys [::send] :as a} msg]
  (if send
    (do
      (send msg)
      a)
    (throw (ex-info "Not a valid actor" {:cmd '|> :actor a}))))

(defn stop [engine]
  (|> engine done))

(defn |< [{:keys [::receive] :as a} create-flow & {:keys [signal?] :or {signal? false}}]
  (if receive
    (create-flow (if signal?
                   (m/signal receive)
                   receive))
    (m/ap
     (m/amb nil)
     (throw (ex-info "Not a valid actor" {:cmd '|< :actor a})))))

(defn >->< [flow]
  (->> flow
       (m/eduction (take 1))
       (m/reduce f/latest)))

(defn flow [{:keys [::receive]}]
  receive)

(defn quiescent?< [a]
  (->> a
       flow
       (m/reductions = nil)
       (m/eduction (take 1))))

(defn quiescent?> [a]
  (m/sp (m/? (quiescent?< a))))

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
    (throw (ex-info "Not a valid actor" {:cmd '|!> :actor a}))))

(defn ->atom
  ([actor]
   (->atom actor (atom nil)))
  ([actor atom]
   (f/drain (|< actor (|<= (map (partial reset! atom))))
            ::->atom)
   atom))

(defn actor? [a]
  (and (map? a)
       (contains? a ::send)))

(defn quiescent? [x]
  (if-let [!quiescent? (::!quiescent? x)]
    @!quiescent?
    (do (log/warn {:message "no quiescent?"
                   :x x})
        true)))
