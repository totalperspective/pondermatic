(ns pondermatic.portal.api
  (:require [portal.api :as p]
            [clojure.datafy :as datafy]))

(def submit (comp p/submit datafy/datafy))

; Add portal as a tap> target
(defn start [launcher]
  (case launcher
    nil (p/open)
    (p/open {:launcher launcher}))
  (add-tap #'submit)
  p/close)
