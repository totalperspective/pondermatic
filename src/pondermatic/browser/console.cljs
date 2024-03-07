(ns pondermatic.browser.console)

(defn trace [label & args]
  (js/console.groupCollapsed label (js/Date.))
  (apply js/console.log args)
  (js/console.trace)
  (js/console.groupEnd))
