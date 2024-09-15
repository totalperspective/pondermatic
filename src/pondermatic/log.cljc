(ns pondermatic.log
  (:require #?(:cljs [pondermatic.portal.utils :as p.util])
            #?(:cljs [pondermatic.browser.console :as console])
            #?(:node [zuko.logging :as log])))

#?(:cljs
   (defn console-tap []
     (add-tap (fn [{:keys [level] :as entry}]
                (let [entry (p.util/datafy-value entry)]
                  (condp = level
                    :trace (console/trace (prn-str entry) entry)
                    :debug (js/console.debug (prn-str entry) entry)
                    :info (js/console.info (prn-str entry) entry)
                    :warn (js/console.warn (prn-str entry) entry)
                    :error (js/console.error (prn-str entry) entry)
                    :fatal (js/console.error (prn-str entry) entry)))))))

#?(:node
   (defn log-tap []
     (add-tap (fn [{:keys [level] :as entry}]
                (let [entry (p.util/datafy-value entry)]
                  (condp = level
                    :trace (log/trace entry)
                    :debug (log/debug entry)
                    :info (log/info entry)
                    :warn (log/warn entry)
                    :error (log/error entry)
                    :fatal (log/fatal entry)))))))

(defn log-level [level]
  #?(:cljs
     (enable-console-print!)
     :node
     (log/set-enabled! true))
  #?(:node
     (log/set-logging-level! level)))
