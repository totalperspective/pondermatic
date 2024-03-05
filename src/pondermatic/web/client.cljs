(ns pondermatic.web.client
  (:require [pondermatic.data :as data]
            [promesa.core :as p]
            [portal.console :as log]
            [pondermatic.flow :as flow]
            [pondermatic.portal.utils :as p.util]))

(def !worker (atom nil))

(def !ids (atom {nil true}))

(defn post [cmd & msg]
  (when-not @!worker
    (throw (ex-info "No worker registered" {})))
  (try
    (let [id (random-uuid)
          p (p/deferred)]
      (swap! !ids assoc id p)
      (let [t-msg (data/write-transit (p.util/datafy-value [id cmd msg]))]
        (js/console.debug "<-window" t-msg)
        (.. @!worker (postMessage t-msg)))
      p)
    (catch js/Error e
      (log/error e))))

(defn post> [& args]
  (flow/await-promise (apply post args)))

(defn generator [id]
  (swap! !ids assoc id (p/deferred))
  #js {:next #(get @!ids id)
       :cancel #(post [:dispose] id)})

(defn handle-msg [[id cmd msg]]
  (when-let [p (get @!ids id)]
    (when id
      (swap! !ids dissoc id))
    (condp = cmd
      :tap (tap> (update-in msg [:worker :result] p.util/pprint))
      :error (log/error msg)
      :throw (let [[msg data] msg
                   e (ex-info msg data)]
               (p/reject! p e))
      :result (p/resolve! p msg)
      :flow (p/resolve! p (generator msg))
      :item (let [done? (= :done msg)]
              (when-not done? (swap! !ids assoc id (p/deferred)))
              (p/resolve! p (clj->js {:value (when-not done? msg)
                                      :done done?})))
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
