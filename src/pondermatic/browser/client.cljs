(ns pondermatic.browser.client
  (:require [pondermatic.flow.port :as !]
            [missionary.core :as m]
            [portal.console :as log]
            [pondermatic.flow :as flow]
            [pondermatic.browser.console :as console])
  (:import [missionary Cancelled]))

(def >worker! (delay (!/!use->port! ::!/port.worker :throw? false)))

(defn post< [& args]
  (let [<return! (m/dfv)
        msg (into [<return!] args)]
    (!/send! @>worker! msg)
    <return!))

(defn post [& args]
  (flow/run
   (apply post< args)
   ::post))

(defn post> [& args]
  (m/ap (let [<next (m/? (apply post< args))
              ><next (m/seed (repeat <next))
              !id (atom nil)]
          (try
            (let [<next (m/?> ><next)
                  {:keys [msg id done?] :as item} (m/? <next)]
              (console/trace ::item item)
              (reset! !id id)
              (when done?
                (throw Cancelled))
              msg)
            (catch Cancelled _
              (log/warn (ex-info "Post> cancelled" {:args args}))
              (when @!id
                (post [:dispose!] [@!id])))))))
