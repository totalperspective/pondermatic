(ns pondermatic.portal.client
  (:require #?(:clj
               [portal.client.jvm :as p]
               :cljs
               [portal.client.node :as p])
            #?(:browser
               [portal.client.web :as pw])
            [portal.console :as log]
            [pondermatic.portal.utils :as p.utils]))

(def port 5678)
(def host "localhost")

(def !opts (atom {:port port :host host}))

(def submit-impl
  #?(:browser (if js/window
                pw/submit
                p/submit)
     :default p/submit))

(defn submitter [submit-impl]
  (fn [value]
    (try
      (->> value
           p.utils/datafy-value
           submit-impl)
      (catch #?(:cljs js/Error :default Exception) e
        #?(:cljs
           (js/console.error e)
           :default
           (println e))))))

(def submit (submitter #(submit-impl @!opts %)))

(defn start [& [launcher]]
  (when launcher
    (reset! !opts {}))
  (add-tap #'submit)
  (log/info !opts))
