(ns pondermatic.browser.handler
  (:require [pondermatic.data :as data]
            [portal.console :as log]
            [pondermatic.flow :as flow]
            [pondermatic.portal.utils :as p.util]
            [pondermatic.flow.port :as !]
            [missionary.core :as m]
            [pondermatic.portal.client :as p]))

(def !ids (atom {nil true}))

(defn poster [worker]
  (fn [[<return! cmd args & [agent]]]
    (prn [<return! cmd args [agent]])
    (try
      (let [id (random-uuid)]
        (swap! !ids assoc id <return!)
        (let [t-msg (data/write-transit (p.util/datafy-value [id cmd args agent]))]
          (js/console.trace "window->" t-msg)
          (.. worker (postMessage t-msg))))
      (catch js/Error e
        (<return! e)
        (log/error e)))))

(defn handle-msg [[id cmd msg]]
  (when-let [<return! (get @!ids id)]
    (when id
      (swap! !ids dissoc id)
      (log/trace {::id id ::cmd cmd ::msg msg ::p <return!}))
    (condp = cmd
      :prn (apply prn msg)
      :tap (tap> (p.util/log (assoc msg :runtime :portal)))
      :error (log/error msg)
      :throw (let [[msg data] msg
                   e (ex-info msg data)]
               (<return! e))
      :result (<return! msg)
      :flow (let [mbox (m/mbx)]
              (swap! !ids assoc msg mbox)
              (<return! mbox))
      :item (let [done? (= :done msg)]
              (when-not done? (swap! !ids assoc id <return!))
              (<return! (if done? ::done msg)))
      (log/warn (ex-info "Couldn't handle message"
                         {:cmd cmd :msg msg})))))


(defn init []
  (enable-console-print!)
  (prn "Web worker - handler startng")
  (p/start)
  (let [location (or (.-location js/globalThis)
                     (.-__dirname js/globalThis)
                     js/import.meta.url)
        worker (js/Worker. "/js/worker.js" location)
        >window! (!/->>port! ::!/worker)
        >worker! (!/!use->port! ::!/worker)]
    (log/info {::worker worker})
    (flow/drain-using >worker!
                      ::handle-msg
                      (flow/tapper handle-msg))
    (flow/drain-using >window!
                      ::post-worker
                      (flow/tapper (poster worker)))
    (.. worker (addEventListener "message"
                                 (fn [^js e]
                                   (let [msg (.. e -data)]
                                     (js/console.debug "->window" msg)
                                     (!/send! >window! (data/read-transit msg))))))))
