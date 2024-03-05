(ns pondermatic.web.worker
  (:require [pondermatic.pool :as pool]
            [pondermatic.engine :as engine]
            [pondermatic.db :as db]
            [pondermatic.rules :as rules]
            [pondermatic.flow :as flow]
            [pondermatic.data :as data]
            [missionary.core :as m]
            [portal.console :as log]
            [pondermatic.core :as p]
            [pondermatic.portal.utils :as p.util]
            [pondermatic.log :as p.log])
  (:import [missionary Cancelled]))

(enable-console-print!)

(defn -post [& msg]
  (try
    (-> msg p.util/datafy-value data/write-transit js/postMessage)
    (catch js/Error e
      (js/console.error e))))

(defn post [& msg]
  (js/console.debug "<-worker" msg)
  (apply -post msg))

(defonce pool (-> {}
                  (pool/contructor :db db/->conn db/clone>)
                  (pool/contructor :rules rules/->session rules/clone>)
                  (pool/contructor :engine p/->engine engine/conn>)
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
  (p.log/console-tap)
  (add-tap #(-post nil :tap {:worker (update % :result data/->eql)}))
  (js/self.addEventListener "message"
                            (fn [^js e]
                              (let [msg (.. e -data)]
                                (js/console.debug "->worker" msg)
                                (handle-msg (data/read-transit msg))))))
