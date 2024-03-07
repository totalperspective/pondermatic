(ns pondermatic.browser.console
  (:require [pondermatic.flow :as flow]))

(defn trace [label & args]
  (when flow/*tap-print*
    (js/console.groupCollapsed label (js/Date.))
    (apply js/console.log args)
    (js/console.trace)
    (js/console.groupEnd)))
