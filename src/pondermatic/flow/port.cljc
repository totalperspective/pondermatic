(ns pondermatic.flow.port
  (:require [missionary.core :as m]
            [pondermatic.flow :as flow]))

(def !id->duplex (atom {}))

(defn sender [<mbx!]
  (fn send! [msg]
    (<mbx! msg)))

(defn >port!? [>port!]
  (and (contains? >port! :send!)
       (contains?  >port! :>recv)))

(defn guard->port! [>port!]
  (when-not (>port!? >port!)
    (throw (ex-info "Invalid Port" {:>port! >port!}))))

(defn duplex->>port! [{:keys [::<a! ::<b! ::id] :as duplex} & {:keys [reverse?] :or {reverse? false}}]
  (when duplex
    {:send! (sender (if reverse? <b! <a!))
     :>recv (flow/mbx> (if reverse? <a! <b!) [id reverse?])}))

(defn id->>port! [port-id]
  (let [<mbx-a! (m/mbx)
        <mbx-b! (m/mbx)
        duplex {::id port-id
                ::<a! <mbx-a!
                ::<b! <mbx-b!}]
    (swap! !id->duplex assoc port-id duplex)
    (duplex->>port! duplex)))

(defn !use->port! [port-id & {:keys [throw?] :or {throw? true}}]
  (let [>port! (duplex->>port! (get @!id->duplex port-id)
                               :reverse? true)]
    (cond
      (>port!? >port!) >port!
      throw? (throw (ex-info "Unknown Port" {:id port-id}))
      :else nil)))

(defn send! [>port! msg]
  (guard->port! >port!)
  (let [{:keys [send!]} >port!]
    (send! msg)))

(defn recv> [>port!]
  (guard->port! >port!)
  (let [{:keys [>recv]} >port!]
    >recv))

(defn >buffer! [size >port!]
  (guard->port! >port!)
  (update >port! :>recv (partial m/buffer size)))
