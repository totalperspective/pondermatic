(ns pondermatic.flow.port
  (:require [missionary.core :as m]
            [pondermatic.flow :as flow]))

(def !ports (atom {}))

(defn sender [<mbx!]
  (fn send! [msg]
    (<mbx! msg)))

(defn ->>port! [port-id]
  (let [<send! (m/mbx)
        <recv! (m/mbx)]
    (swap! !ports assoc port-id {:send! (sender <send!)
                                 :>recv (flow/mbx> <recv!)})
    {:send! (sender <recv!)
     :>recv (flow/mbx> <send!)}))

(defn >port!? [>port!]
  (and (contains? >port! :send!)
       (contains?  >port! :>recv)))

(defn guard->port! [>port!]
  (when-not (>port!? >port!)
    (throw (ex-info "Invalid Port" {:>port! >port!}))))

(defn !use->port! [port-id & {:keys [throw?] :or {throw? true}}]
  (let [>port! (get @!ports port-id)]
    (cond
      (>port!? >port!) >port!
      throw? (throw (ex-info "Unknown Port" {:id port-id}))
      :else nil)))

(defn send! [>port! msg]
  (prn ::send! msg)
  (guard->port! >port!)
  (let [{:keys [send!]} >port!]
    (prn ::send! send! msg)
    (send! msg)))

(defn recv> [>port!]
  (guard->port! >port!)
  (let [{:keys [>recv]} >port!]
    >recv))

(defn >buffer! [size >port!]
  (guard->port! >port!)
  (update >port! :>recv (partial m/buffer size)))
