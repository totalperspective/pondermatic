(ns pondermatic.browser.worker
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
            [pondermatic.log :as p.log]
            [pondermatic.flow.port :as !]
            [pondermatic.browser.console :as console]
            [clojure.walk :as w])
  (:import [missionary Cancelled]))

(enable-console-print!)

(def >window! (!/>buffer! 100 (!/id->>port! ::port)))

(defn post-message [port t-msg]
  (console/trace "worker->" t-msg)
  (.. port (postMessage t-msg)))

(defn post [& msg]
  (!/send! >window! msg))

(defonce pool (-> {}
                  (pool/contructor :db db/->conn db/clone>)
                  (pool/contructor :rules rules/->session rules/clone>)
                  (pool/contructor :engine p/->engine p/clone>)
                  pool/->pool))

(def !flows (atom {}))

(def with-agent< (partial pool/with-agent< pool))

(defn flow [flow><]
  (with-meta flow>< {:flow? true}))

(def cmd->fun {:to-pool! (fn [& args]
                           (apply pool/to-pool! args)
                           nil)
               :dispose (fn [id]
                          (let [dispose! (get @!flows id)]
                            (dispose!)))
               :pool {:add-agent pool/add-agent!
                      :copy-agent pool/copy-agent!
                      :remove-agent pool/remove-agent!
                      :to-agent pool/to-agent!}
               :engine {:q>< (flow (with-agent< engine/q><))
                        :entity>< (flow (with-agent< engine/entity><))
                        :entity< (with-agent< engine/entity<)}})

(defn prn> [& msg]
  (post nil :prn msg))

(defn handle-msg [[id cmd args agent]]
  (let [fun (get-in cmd->fun cmd)
        {:keys [:flow?]} (meta fun)
        info {::id id ::cmd cmd ::fun? (boolean fun)
              ::args args ::agent agent ::flow? flow?}]
    (log/trace info)
    (if-not fun
      (post id :throw ["Unknown Command" {:cmd cmd :msg :msg}])
      (try
        (cond
          (and agent flow?)
          (let [<>flow (fun agent args)
                msg-id (random-uuid)
                done! (fn [x]
                        (log/trace (assoc info ::success? (fn? x)))
                        (when-not (fn? x)
                          (post msg-id :item :done)
                          (swap! !flows dissoc msg-id)))
                <drain (m/sp (try (let [>flow (m/? <>flow)]
                                    (flow/drain-using
                                     >flow
                                     info
                                     (flow/tapper
                                      (fn [item]
                                        (log/trace (assoc info ::item item))
                                        (post msg-id :item item)))))
                                  (catch Cancelled e
                                    (log/warn (ex-info "Cmd fkow cancelled" info))
                                    (done! e))))]
            (swap! !flows assoc msg-id <>flow)
            (post id :flow msg-id)
            (<drain done! done!))
          agent
          (let [<task (m/sp (let [<result (fun agent args)]
                              (m/? <result)))]
            (<task #(post id :result %)
                   #(post id :result (if (instance? Cancelled  %)
                                       (ex-info "Cancelled" info)
                                       %))))
          :else
          (let [result (apply fun pool args)]
            (post id :result result)))
        (catch js/Error e
          (post id :throw [(ex-message e) (ex-data e)]))))))

(defn intern-callbacks [msg]
  (w/postwalk (fn [node]
                (if (and (map? node) (::!/fn node))
                  (let [id (::!/fn node)]
                    (fn [msg]
                      (post id :result msg)))
                  node))
              msg))

(defn ->>post-message [>port! window]
  (->> >port!
       !/recv>
       (m/eduction
        (map (comp (partial post-message window)
                   data/write-transit
                   p.util/datafy-value)))))

(defn ->>recv-message [>port!]
  (->> >port!
       !/recv>
       (m/eduction
        (map (comp handle-msg
                   intern-callbacks
                   data/read-transit)))))

(defn init []
  (prn "Web worker startng")
  (p.log/console-tap)
  (add-tap #(post nil :tap (update % :result data/->log-safe)))
  (let [>worker! (!/>buffer! 100 (!/!use->port! ::port))
        >post-worker (->>post-message >worker! js/globalThis)
        >recv-message (->>recv-message >window!)]
    (flow/drain >recv-message ::recv-message)
    (flow/drain >post-worker ::post-worker)
    (js/self.addEventListener "message"
                              (fn [^js e]
                                (let [msg (.. e -data)]
                                  (console/trace "->worker" msg)
                                  (!/send! >worker! msg)))))
  (prn "Web worker started"))
