(ns pondermatic.log.winston
  (:require ["winston" :as winston]
            ["winston-transport" :as winston-transport]
            [cljs.pprint :as pp]))

(defn format-log-message [message]
  (cond
    (string? message) message
    (map? message) (with-out-str (pp/pprint message))
    :else (pr-str message)))

(def custom-formatter
  (winston/format
   (fn [info _opts]
     (let [clj-info (js->clj info :keywordize-keys true)
           formatted-message (format-log-message (:message clj-info))
           log-data (assoc clj-info
                           :level (keyword (:level clj-info))
                           :message formatted-message)]
       (clj->js log-data)))))

(defn create-console-transport []
  (winston-transport.
   #js {:log (fn [info callback]
               (let [log-data (js->clj info :keywordize-keys true)]
                 (case (:level log-data)
                   :error (js/console.error (clj->js log-data))
                   :warn (js/console.warn (clj->js log-data))
                   :info (js/console.info (clj->js log-data))
                   :debug (js/console.debug (clj->js log-data))
                   :trace (js/console.trace (clj->js log-data))
                   (js/console.log (clj->js log-data)))
                 (callback)))
        :name "console"}))

(def logger
  (winston/createLogger
   (clj->js
    {:level "verbose"
     :format (winston/format.combine
              (winston/format.timestamp)
              (custom-formatter))
     :transports [(create-console-transport)]})))

(defn log [level message & [meta]]
  (let [base-meta {:ns (or (:ns meta) (namespace ::x) "pondermatic")
                   :time (.toISOString (js/Date.))
                   :runtime "cljs"}
        full-meta (merge base-meta meta)]
    (try
      (.log logger (name level) message (clj->js full-meta))
      (catch js/Error e
        (js/console.error "Logging error:" (.message e)))))) ; Added error handling

(defn winston-tap [x]
  (if (and (map? x) (:level x))
    (let [{:keys [level message]} x
          level (condp = level
                  :fatal :error
                  :error :error
                  :warn :warn
                  :info :info
                  :debug :debug
                  :trace :verbose
                  level)]
      (log level message (dissoc x :level :message)))
    (log :info x)))
