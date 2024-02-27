(ns build
  (:require [zuko.io :refer [spit]]))

(defn hook
  {:shadow.build/stage :flush}
  [build-state]
  (let [build (-> build-state
                  :shadow.build/build-id
                  name)
        output (str "." build ".source-map.edn")
        sms (->> build-state
                 :output
                 (reduce (fn [m [_ out]]
                           (if m
                             (let [{:keys [ns :source-map-compact]} out]
                               (assoc m ns source-map-compact))
                             m))
                         {}))]
    (spit output sms))
  build-state)
