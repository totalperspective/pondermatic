(ns pondermatic.portal.client
  (:require #?(:clj
               [portal.client.jvm :as p]
               :cljs
               [portal.client.node :as p])
            [clojure.datafy :as datafy]
            [portal.console :as log]))

(def port 5678)

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
  (p/submit {:port port}
            (if-not (exception? value)
              (datafy/datafy value)
              (error->data value))))

(defn start [_launcher]
  (add-tap #'submit)
  (tap> :started....)
  (log/info {:port port}))
