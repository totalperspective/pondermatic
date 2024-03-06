(ns pondermatic.web.worker
  (:require [pondermatic.pool :as pool]
            [pondermatic.engine :as engine]
            [pondermatic.db :as db]
            [pondermatic.rules :as rules]
            [pondermatic.flow :as flow]
            [pondermatic.data :as data]
            [missionary.core :as m]
            [zuko.logging :as log])
  (:import [missionary Cancelled]))

(defn -post [& msg]
  (-> msg data/write-transit js/postMessage))

(defn post [& msg]
  (log/trace {:<-worker msg})
  (apply -post msg))

(def pool (-> {}
              (pool/contructor :db db/->conn db/clone>)
              (pool/contructor :rules rules/->session rules/clone>)
              (pool/contructor :engine engine/->engine engine/conn>)
              pool/->pool))

(def !flows (atom {}))

(def with-agent< (partial pool/with-agent< pool))

(def cmd->fun {:dispose (fn [id]
                          (let [dispose! (get @!flows id)]
                            (dispose!)))
               :pool {:add-agent pool/add-agent!
                      :copy-agent pool/copy-agent!
                      :remove-agent pool/remove-agent!
                      :to-agent pool/to-agent!}
               :engine {:q>< (with-agent< engine/q><)
                        :entity>< (with-agent< engine/entity><)}})

(defn handle-msg [[id cmd args agent]]
  (let [fun (get-in cmd->fun cmd)]
    (if fun
      (try
        (if agent
          (let [info {:agent agent :id id :cmd cmd :args args}
                <>flow (apply fun agent args)
                msg-id (random-uuid)]
            (post id :flow id)
            (swap! !flows assoc msg-id <>flow)
            (flow/drain-using
             (m/ap (try (let [>flow (m/? <>flow)]
                          (loop []
                            (let [item (m/?< >flow)]
                              (post msg-id :item item)
                              (recur))))
                        (catch Cancelled _
                          (post msg-id :item :done)
                          (swap! !flows dissoc msg-id))))
             info
             (flow/tapper #(log/trace (assoc info :item %)))))
          (let [result (apply fun pool args)]
            (post id :result result)))
        (catch js/Error e
          (post id :throw [(ex-message e) (ex-data e)])))
      (post id :throw ["Unknown Command" {:cmd cmd :msg :msg}]))))

(defn init []
  (js/console.log "Web worker startng")
  (add-tap (fn [msg]
             (js/console.debug msg)
             (-post nil :tap {::msg msg})))
  (js/self.addEventListener "message"
                            (fn [^js e]
                              (let [msg (.. e -data)]
                                (log/trace {:->worker msg})
                                (handle-msg (data/read-transit msg))))))
