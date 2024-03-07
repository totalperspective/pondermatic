(ns pondermatic.browser.handler
  (:require [pondermatic.data :as data]
            [portal.console :as log]
            [pondermatic.flow :as flow]
            [pondermatic.portal.utils :as p.util]
            [pondermatic.flow.port :as !]
            [missionary.core :as m]
            [pondermatic.portal.client :as p]
            [pondermatic.browser.console :as console]))

(def !ids (atom {nil true}))

(defn prepare-msg [msg]
  (let [[<return! cmd args & [agent]] msg
        id (random-uuid)]
    (swap! !ids assoc id <return!)
    [id cmd args agent]))

(defn post-message [worker t-msg]
  (console/trace  "window->" t-msg)
  (.. worker (postMessage t-msg)))

(defn handle-msg [[id cmd msg]]
  (let [<return! (get @!ids id)]
    (when id
      (log/trace {::id id ::cmd cmd ::msg msg ::<return! <return!}))
    (when <return!
      (swap! !ids dissoc id))
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

(defn ->>post-message [>port! worker]
  (->> >port!
       !/recv>
       (m/eduction
        (map (comp (partial post-message worker)
                   data/write-transit
                   p.util/datafy-value
                   prepare-msg)))))

(defn ->>recv-message [>port!]
  (->> >port!
       !/recv>
       (m/eduction
        (map (comp handle-msg
                   data/read-transit)))))

(defn init []
  (enable-console-print!)
  (prn "Web worker - handler startng")
  (p/start)
  (let [location (or (.-location js/globalThis)
                     (.-__dirname js/globalThis))
        worker (js/Worker. "/js/worker.js" location)
        port-id ::!/port.worker
        >window! (!/->>port! port-id)
        >worker! (!/!use->port! port-id)
        >post-worker (->>post-message >window! worker)
        >recv-message (->>recv-message >worker!)]
    (log/info {::worker worker})

    (flow/drain >recv-message ::recv-message)
    (flow/drain >post-worker ::post-worker)

    (.. worker (addEventListener "message"
                                 (fn [^js e]
                                   (let [msg (.. e -data)]
                                     (console/trace "->window" msg)
                                     (!/send! >window! msg))))))
  (prn "Web worker - handler started"))
