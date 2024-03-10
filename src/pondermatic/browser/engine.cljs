(ns pondermatic.browser.engine
  (:require [pondermatic.pool :as pool]
            [pondermatic.shell :as sh]
            [portal.console :as log]
            [missionary.core :as m]
            [pondermatic.core :as p]
            [pondermatic.browser.client :as client])
  (:require-macros [portal.console :as log]))

(def worker? (delay (boolean @client/>worker!)))

(def post client/post)
(def post< client/post<)
(def post> client/post>)

(defn forwarder [agent msg]
  (let [{:keys [type args id]} agent]
    (log/trace {::agent agent ::msg msg})
    (if (= sh/done msg)
      (m/sp (m/? (post< [:pool :remove-agent] id))
            agent)
      (let [[cmd msg] (if (map? msg)
                        (first msg)
                        msg)
            <tx (m/sp (let [result (m/? (post< [:pool :to-agent] [id {cmd msg}]))]
                        (log/trace {:agent agent
                                    cmd msg
                                    :result result})
                        agent))]
        (condp = cmd
          ::create (m/sp (let [id (m/? (post< [:pool :add-agent] (apply vector type args)))]
                           (assoc agent :id id)))
          :->db <tx
          :+>db <tx
          :!>db <tx

          (do (log/warn (ex-info "Unknown Command" {::cmd cmd}))
              agent))))))

(defn ->agent [agent]
  (->> agent
       (sh/engine forwarder)
       (sh/actor ::prefix)))

(defn create-local [type & args]
  (let [engine (->agent {:type type :args args})]
    (sh/|> engine {::create type})))

(defn agent> [engine]
  (sh/|!> engine identity))

(defn clone-local [source-agent]
  (m/sp
   (let [agent (m/? (agent> source-agent))
         id (str (random-uuid))]
     (sh/|> source-agent {:clone-to id})
     (->agent (assoc agent :remote id)))))

(defn contructor [cs type create clone]
  (let [create' (fn create' [& args]
                  (if @worker?
                    (apply create-local type args)
                    (apply create args)))
        clone' (fn clone' [& args]
                 (if @worker?
                   (apply clone-local args)
                   (apply clone args)))]
    (pool/contructor cs type create' clone')))

(defn with-local [fun< alias & {:keys [:flow?] :or {flow? false}}]
  (fn remote-fun< [agent & args]
    (if @worker?
      (m/sp (let [id (m/? (sh/|!> agent :id))]
              (if flow?
                (post> [:engine alias] args id)
                (m/? (post< [:engine alias] args id)))))
      (apply fun< agent args))))

(def to-pool! (fn [pool & args]
                (if @worker?
                  (post [:to-pool!] args)
                  (apply pool/to-pool! pool args))))

(def q>< (with-local p/q>< :q>< :flow? true))

(def entity>< (with-local p/entity>< :entity>< :flow? true))

(def entity< (with-local p/entity< :entity<))
