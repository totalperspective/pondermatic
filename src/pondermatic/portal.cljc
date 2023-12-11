(ns pondermatic.portal
  (:require [portal.api :as p]
            ;; #?(:cljs [portal.web :as pw])
            [clojure.datafy :as datafy]))

(def submit (comp p/submit datafy/datafy))
; Add portal as a tap> target
(defn start [launcher]
  (case launcher
    nil (p/open)
    ;; :browser (pw/open)
    (p/open {:launcher launcher}))
  (add-tap #'submit)
  p/close)
