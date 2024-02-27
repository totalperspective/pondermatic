(ns pondermatic.log
  (:require [pondermatic.portal.utils :as p.util]
            [zuko.logging :as log]))


(defn log-tap []
  (add-tap (fn [{:keys [level] :as entry}]
             (let [entry (p.util/datafy-value entry)]
               (condp = level
                 :trace (log/trace entry)
                 :debug (log/debug entry)
                 :info (log/info entry)
                 :warn (log/warn entry)
                 :error (log/error entry)
                 :fatal (log/fatal entry))))))

(defn log-level [level]
  #?(:cljs
     (enable-console-print!)
     :default
     (log/set-enabled! true))
  (log/set-logging-level! level))
