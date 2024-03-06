(ns pondermatic.browser.client
  (:require [pondermatic.flow.port :as !]
            [missionary.core :as m]
            [pondermatic.flow :as flow]))

(def >worker! (delay (!/!use->port! ::!/worker :throw? false)))

(defn post< [& args]
  (let [<return! (m/dfv)]
    (!/send! @>worker! (into [<return!] args))
    <return!))

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
