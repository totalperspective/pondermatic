(ns pondermatic.pool
  (:require [pondermatic.shell :as sh]
            [portal.console :as log]
            [missionary.core :as m]))

(defn pool-process
  [session cmd]
  (if-not (= cmd sh/done)
    (let [{:keys [+agent -agent ->agent =agent]} cmd]
      (cond
        +agent (let [{:keys [id agent args]} +agent
                     {:keys [create clone]} (get-in session [::contructors agent])]
                 (if create
                   (assoc-in session [::agents id] {:agent (apply create args) :clone clone})
                   (do (log/warn {:message "Unkown agent type" :id agent})
                       session)))
        -agent (let [{:keys [id]} -agent
                     agent (get-in session [::agents id :agent])]
                 (if agent
                   (do
                     (sh/stop agent)
                     (update session ::agents dissoc id))
                   (do
                     (log/warn {:message "Unkown agent" :id id})
                     session)))
        ->agent (let [{:keys [id cmd]} ->agent
                      agent (get-in session [::agents id :agent])]
                  (if agent
                    (sh/|> agent cmd)
                    (log/warn {:message "Unkown agent" :id id}))
                  session)
        =agent (let [{:keys [source target]} =agent
                     {:keys [agent clone]} (get-in session [::agents source])]
                 (if (and agent clone)
                   (m/sp
                    (->> agent
                         clone
                         m/?
                         (assoc {:clone clone} :agent)
                         (assoc-in session [::agents target])))
                   (do
                     (log/warn {:message "Unkown agent or not cloneable" :id source}))))
        :else (log/warn {:message "Unkown command" :cmd cmd})))
    (reduce-kv (fn [a k agent]
                 (sh/stop agent)
                 (conj a k))
               []
               (::agents session))))

(defn contructor [cs type create clone]
  (assoc cs type {:create create :clone clone}))

(defn ->pool [contructors]
  (let [session {::contructors contructors :agents {}}]
    (->> session
         (sh/engine pool-process)
         sh/actor)))

(defn add-agent! [pool agent & args]
  (let [id (random-uuid)]
    (sh/|> pool {:+agent {:id id :agent agent :args args}})
    id))

(defn remove-agent! [pool id]
  (sh/|> pool {:-agent {:id id}}))

(defn to-agent! [pool id cmd]
  (sh/|> pool {:->agent {:id id :cmd cmd}}))

(defn copy-agent! [pool src]
  (let [tgt (random-uuid)]
    (sh/|> pool {:=agent {:souce src :target tgt}})
    tgt))
