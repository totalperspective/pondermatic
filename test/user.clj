(ns user
  (:require [cognitect.test-runner.api]
            [pondermatic.log :as log]))

(log/log-level :warn)

(defn run-test [opts]
  (cognitect.test-runner.api/test opts))
