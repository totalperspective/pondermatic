(ns pondermatic.web.engine
  (:require [pondermatic.pool :as pool]
            [pondermatic.shell :as sh]
            [portal.console :as log]
            [missionary.core :as m]
            [pondermatic.web.client :as client]
            [pondermatic.core :as p])
  (:require-macros [portal.console :as log]))

(pondermatic.web.client/init)

(def worker? (boolean @client/!worker))

(defn forwarder [agent msg]
  (let [{:keys [type args id]} agent]
    (log/trace {::agent agent ::msg msg})
    (if (= sh/done msg)
      (m/sp (m/? (client/post< [:pool :remove-agent] id))
            agent)
      (let [[cmd msg] (if (map? msg)
                        (first msg)
                        msg)]
        (condp = cmd
          ::create (m/sp (let [id (m/? (client/post< [:pool :add-agent] (apply vector type args)))]
                           (assoc agent :id id)))
          :->db (m/sp (let [result (m/? (client/post< [:pool :to-agent] [id {cmd msg}]))]
                        (log/trace {:agent agent
                                    cmd msg
                                    :result result})
                        agent))

          (do (log/warn (ex-info "Unknown Command" {:cmd cmd}))
              agent))))))

(defn ->agent [agent]
  (->> agent
       (sh/engine forwarder)
       sh/actor))

(defn create-local [type & args]
  (let [engine (->agent {:type type :args args})]
    (sh/|> engine {::create type})))

(defn agent> [engine]
  (sh/|!> engine identity))

(defn clone-local [source-agent]
  (m/sp
   (let [agent (m/? (agent> source-agent))
         id (random-uuid)]
     (sh/|> source-agent {:clone-to id})
     (->agent (assoc agent :remote id)))))

(defn contructor [cs type create clone]
  (log/trace {::cs cs ::type type})
  (let [create' (if worker?
                  (partial create-local type)
                  create)
        clone' (if worker?
                 clone-local
                 clone)]
    (pool/contructor cs type create' clone')))

(defn with-local [fun< alias]
  (if worker?
    (fn remote-fun< [agent & args]
      (m/sp (let [id (m/? (sh/|!> agent :id))]
              (client/post> [:engine alias] args id))))
    fun<))

(def q>< (with-local p/q>< :q><))

(def entity>< (with-local p/entity>< :entity><))
