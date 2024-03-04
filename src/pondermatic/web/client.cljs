(ns pondermatic.web.client
  (:require [pondermatic.data :as data]
            [promesa.core :as p]
            [portal.console :as log]))

(def !worker (atom nil))

(def !ids (atom {}))

(defn post [cmd & msg]
  (when-not @!worker
    (throw (ex-info "No worker registered" {})))
  (let [id (random-uuid)]
    (swap! !ids assoc id (p/deferred))
    (.. @!worker (postMessage (data/write-transit [id cmd msg])))))

(defn generator [id]
  (swap! !ids assoc id (p/deferred))
  #js {:next #(get @!ids id)
       :cancel #(post [:dispose] id)})

(defn handle-msg [[id cmd msg]]
  (when-let [p (get @!ids id)]
    (swap! !ids dissoc id)
    (condp = cmd
      :throw (let [[msg data] msg
                   e (ex-info msg data)]
               (p/reject! p e))
      :result (p/resolve! p msg)
      :flow (p/resolve! p (generator msg))
      :item (let [done? (= :done msg)]
              (when-not done? (swap! !ids assoc id (p/deferred)))
              (p/resolve! p (clj->js {:value (when-not done? msg)
                                      :done done?})))
      :else (log/warn (ex-info "Couldn't handle message"
                               {:cmd cmd :msg msg})))))

(defn init []
  (let [worker (js/Worker. "./worker.js")]
    (reset! !worker worker)
    (set! js/globalThis.pondermaticPost (fn [msg]
                                          (apply post (data/read-transit msg))))
    (.. worker (addEventListener "message"
                                 (fn [^js e]
                                   (handle-msg (data/read-transit (.. e -data))))))))
