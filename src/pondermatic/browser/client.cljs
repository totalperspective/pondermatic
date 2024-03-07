(ns pondermatic.browser.client
  (:require [pondermatic.flow.port :as !]
            [missionary.core :as m]
            [pondermatic.flow :as flow])
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
              {:keys [id]} (meta <next)]
          (try
            (prn ::id id)
            (loop []
              (let [value (m/? <next)]
                (when-not (= value ::done)
                  (m/amb value (recur)))))
            (catch Cancelled _
              (post [:dispose!] [id]))))))
