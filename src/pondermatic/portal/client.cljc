(ns pondermatic.portal.client
  (:require #?(:clj
               [portal.client.jvm :as p]
               :cljs
               [portal.client.node :as p])
            #?(:cljs
               [portal.client.web :as pw])
            [portal.console :as log]
            [pondermatic.portal.utils :as p.utils]
            #?(:cljs [pondermatic.env :as env])))

(def port 5678)
(def host "localhost")

(def !opts (atom {:port port :host host}))

#?(:cljs
   (defn async-submit [opts value]
     (let [submit-impl (if env/browser?
                         pw/submit
                         p/submit)]
       (if-not (instance? js/Promise value)
         (submit-impl opts value)
         (-> value
             (.then #(submit-impl opts %))
             (.catch #(submit-impl opts %)))))))

(def submit-impl
  #?(:cljs async-submit
     :default p/submit))

(defn submitter [submit-impl]
  (fn [value]
    (try
      (->> value
           p.utils/datafy-value
           submit-impl)
      (catch #?(:cljs js/Error :default Exception) e
        #?(:cljs
           (if env/browser?
             (js/console.error e)
             (println e))
           :default
           (println e))))))

(def submit (submitter #(submit-impl @!opts %)))

(defn start [& [launcher]]
  (when launcher
    (reset! !opts {}))
  (add-tap #'submit)
  (log/info !opts))
