(ns pondermatic.web.engine
  (:require [pondermatic.pool :as pool]
            [pondermatic.shell :as sh]
            [portal.console :as log]
            [missionary.core :as m]
            [pondermatic.web.client :as client])
  (:require-macros [portal.console :as log]))

;; (pondermatic.web.client/init)

(def worker? (boolean @client/!worker))

(defn forwarder [agent [cmd & msg]]
  (log/trace {::agent agent ::cmd cmd ::msg msg})
  (let [{:keys [type args]} agent]
    (condp = cmd
      ::create (m/sp (let [id (m/? (apply client/post> [:pool :add-agent] type args))]
                       (log/trace {::id id})
                       (assoc agent ::id id)))
      :else (do (log/warn (ex-info "Unknown Command" {:cmd cmd}))
                agent))))

(defn ->agent [agent]
  (->> agent
       (sh/engine forwarder)
       sh/actor))

(defn create-local [type & args]
  (let [engine (->agent {:type type :args args})]
    (sh/|> engine [::create])))

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
