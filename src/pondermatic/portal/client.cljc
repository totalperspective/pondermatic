(ns pondermatic.portal.client
  (:require #?(:clj
               [portal.client.jvm :as p]
               :cljs
               [portal.client.node :as p])
            [clojure.datafy :as datafy]
            [portal.console :as log]))

(def port 5678)

(def submit (comp (partial p/submit {:port port}) datafy/datafy))

(defn start [_launcher]
  (add-tap #'submit)
  (tap> :started....)
  (log/info {:port port}))
