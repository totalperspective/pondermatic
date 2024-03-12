(ns pondermatic.pool
  (:require [pondermatic.shell :as sh]
            [portal.console :as log]
            [missionary.core :as m]
            [pondermatic.browser.console :as console]
            [pondermatic.flow :as flow]))

(defn pool-process
  [session cmd]
  (log/trace {::cmd cmd ::session session})
  (if-not (= cmd sh/done)
    (let [[[cmd msg]] (seq cmd)]
      ((flow/tap cmd) msg)
      (condp = cmd
        :agents (let [{:keys [cb]} msg
                      {:keys [::agents]} session
                      info (reduce-kv (fn [m id {:keys [type]}]
                                        (assoc m id type))
                                      {} agents)]
                  (log/trace {:agents info})
                  (when (fn? cb)
                    (cb info))
                  session)
        :reset! (let [{:keys [agents]} session]
                  (reduce (fn [session [id]]
                            (pool-process session {:-agent {:id id}}))
                          session
                          agents))
        :+agent (let [{:keys [id agent args]} msg
                      {:keys [create clone]} (get-in session [::contructors agent])]
                  (if create
                    (assoc-in session [::agents id] {:type agent :agent (apply create args) :clone clone})
                    (do (log/warn (ex-info "Unkown agent type" {:agent agent :id id}))
                        session)))
        :-agent (let [{:keys [id]} msg
                      agent (get-in session [::agents id :agent])]
                  (if agent
                    (do
                      (sh/stop agent)
                      (update session ::agents dissoc id))
                    (do
                      (log/warn (ex-info "Unkown agent" {:id id}))
                      session)))
        :->agent (let [{:keys [id cmd]} msg
                       agent (get-in session [::agents id :agent])]
                   (if agent
                     (sh/|> agent cmd)
                     (log/warn (ex-info "Unkown agent" {:id id})))
                   session)
        :=agent (let [{:keys [source target]} msg
                      {:keys [agent clone type]} (get-in session [::agents source])]
                  (if (and agent clone)
                    (m/sp
                     (->> agent
                          clone
                          m/?
                          (assoc {:clone clone :type type} :agent)
                          (assoc-in session [::agents target])))
                    (do
                      (log/warn (ex-info "Unkown agent or not cloneable" {:id source}))
                      session)))
        (do (log/warn (ex-info "Unkown Command" {::cmd cmd ::msg msg}))
            session)))
    (reduce-kv (fn [a k agent]
                 (sh/stop agent)
                 (conj a k))
               []
               (::agents session))))

(defn agent> [pool id]
  (sh/|!> pool #(get-in % [::agents id :agent])))

(defn contructor [cs type create clone]
  (assoc cs type {:create create :clone clone}))

(defn ->pool [contructors]
  (let [session {::contructors contructors ::agents {}}]
    (->> session
         (sh/engine pool-process)
         (sh/actor ::prefix))))

(defn add-agent! [pool agent & args]
  (let [id (str (random-uuid))
        cmd {:+agent {:id id :agent agent :args args}}]
    (log/debug cmd)
    (sh/|> pool cmd)
    id))

(defn remove-agent! [pool id]
  (sh/|> pool {:-agent {:id id}})
  nil)

(defn to-agent! [pool id cmd]
  (sh/|> pool {:->agent {:id id :cmd cmd}})
  id)

(defn to-pool! [pool msg]
  (sh/|> pool msg))

(defn copy-agent!
  ([pool src]
   (copy-agent! pool src (str (random-uuid))))
  ([pool src tgt]
   (sh/|> pool {:=agent {:source src :target tgt}})
   tgt))

(defn with-agent< [pool task<]
  (fn [agent args]
    (m/sp (let [agent (m/? (agent> pool agent))]
            (m/? (apply task< agent args))))))
