(ns pondermatic.portal.client
  (:require #?(:clj
               [portal.client.jvm :as p]
               :cljs
               [portal.client.node :as p])
            #?(:browser
               [portal.client.web :as pw])
            [clojure.datafy :as datafy]
            [clojure.core.protocols :as ccp]
            [portal.console :as log]
            [incognito.base :as ib]
            [clojure.walk :as w]
            #?(:cljs
               [java.time :refer [LocalDate LocalDateTime]]))
  #?(:clj
     (:import [java.time LocalDate LocalDateTime])))

(def write-handlers
  {`LocalDate (fn [d] (str d))
   `LocalDateTime (fn [dt] (str dt))})

(extend-protocol
 ccp/Datafiable
  LocalDate
  (datafy [d]
    (ib/incognito-writer write-handlers d))
  LocalDateTime
  (datafy [d]
    (ib/incognito-writer write-handlers d)))

(def port 5678)
(def host "localhost")

(def submit-impl
  #?(:browser (if js/window
                pw/submit
                p/submit)
     :default p/submit))

#?(:cljs
   (defn error->data [ex]
     (merge
      (when-let [data (.-data ex)]
        {:data data})
      {:runtime :cljs
       :cause   (.-message ex)
       :via     [{:type    (symbol (.-name (type ex)))
                  :message (.-message ex)}]
       :stack   (.-stack ex)}))
   :default
   (defn error->data [ex]
     (assoc (datafy/datafy ex) :runtime :clj)))

(defn exception? [e]
  (instance? #?(:cljs js/Error :default Exception) e))

(defn submit [value]
  (->> value
       (w/postwalk (fn [value]
                     (if (exception? value)
                       (error->data value)
                       value)))
       datafy/datafy
       (submit-impl {:port port :host host})))

(defn start [_launcher]
  (add-tap #'submit)
  (log/info {:portal/port port}))
