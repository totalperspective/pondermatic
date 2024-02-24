(ns user
  (:require [cognitect.test-runner.api]
            [zuko.logging :as log]
            [pondermatic.portal.utils :as p.utils]))

(log/set-enabled! true)
(log/set-logging-level! :warn)

(defn run-test [opts]
  (add-tap (fn [{:keys [level] :as entry}]
             (let [entry (p.utils/datafy-value entry)]
               (condp = level
                 :trace (log/trace entry)
                 :debug (log/debug entry)
                 :info (log/info entry)
                 :warn (log/warn entry)
                 :error (log/error entry)
                 :fatal (log/fatal entry)))))
  (cognitect.test-runner.api/test opts))
