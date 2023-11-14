(ns user
  (:require [pondermatic.rules :as rules]
            [pondermatic.db :as db]
            [portal.api :as p]
            [clojure.datafy :as datafy]))

(def submit (comp p/submit datafy/datafy))

(defonce portal (let [p (p/open {:launcher :vs-code})]
                  (add-tap #'submit)
                  p)) ; Add portal as a tap> target

(defn ^:dev/after-load -main
  [& _]
  (rules/run-test)
  (db/run-test))
