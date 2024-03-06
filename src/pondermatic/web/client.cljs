(ns pondermatic.web.client
  (:require [pondermatic.data :as data]
            [portal.console :as log]
            [pondermatic.flow :as flow]
            [pondermatic.portal.utils :as p.util]
            [missionary.core :as m]))

(def !worker (atom nil))

(def !ids (atom {nil true}))

(defn post< [cmd args & [agent]]
  ;; (prn ::cmd cmd ::args args ::agent agent)
  (when-not @!worker
    (throw (ex-info "No worker registered" {})))
  (try
    (let [id (random-uuid)
          p (m/dfv)]
      (swap! !ids assoc id p)
      (let [t-msg (data/write-transit (p.util/datafy-value [id cmd args agent]))]
        (js/console.debug "<-window" t-msg)
        (.. @!worker (postMessage t-msg)))
      p)
    (catch js/Error e
      (log/error e))))

(defn post [& args]
  (flow/run
   (apply post< args)
   ::post))

(defn post> [& args]
  (m/ap (let [<next (m/? (apply post< args))]
          (loop []
            (let [value (m/? <next)]
              (when-not (= value ::done)
                (m/amb value (recur))))))))

(defn handle-msg [[id cmd msg]]
  (when-let [p (get @!ids id)]
    (when id
      (swap! !ids dissoc id)
      (log/trace {::id id ::cmd cmd ::msg msg ::p p}))
    (condp = cmd
      :prn (apply prn msg)
      :tap (tap> (p.util/log (assoc msg :runtime :portal)))
      :error (log/error msg)
      :throw (let [[msg data] msg
                   e (ex-info msg data)]
               (p e))
      :result (p msg)
      :flow (let [mbox (m/mbx)]
              (swap! !ids assoc msg mbox)
              (p mbox))
      :item (let [done? (= :done msg)]
              (when-not done? (swap! !ids assoc id p))
              (p (if done? ::done msg)))
      (log/warn (ex-info "Couldn't handle message"
                         {:cmd cmd :msg msg})))))

(defn init []
  (let [location (or (.-location js/globalThis)
                     (.-__dirname js/globalThis)
                     js/import.meta.url)
        worker (js/Worker. "/js/worker.js" location)]
    (reset! !worker worker)
    (set! js/globalThis.pondermaticPost (fn [msg]
                                          (apply post (data/read-transit msg))))
    (.. worker (addEventListener "message"
                                 (fn [^js e]
                                   (let [msg (.. e -data)]
                                     (js/console.debug "->window" msg)
                                     (handle-msg (data/read-transit msg))))))))
