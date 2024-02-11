(ns pondermatic.portal.client
  (:require #?(:clj
               [portal.client.jvm :as p]
               :cljs
               [portal.client.node :as p])
            #?(:browser
               [portal.client.web :as pw])
            [clojure.datafy :as datafy]
            [portal.console :as log]
            [clojure.walk :as w]))

(def port 5678)
(def host "localhost")

(def !opts (atom {:port port :host host}))

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

(defn submitter [submit-impl]
  (fn [value]
    (->> value
         (w/postwalk (fn [value]
                       (cond
                         (exception? value)
                         (error->data value)

                         (fn? value)
                         (pr-str value)

                         :else
                         value)))
         datafy/datafy
         submit-impl)))

(def submit (submitter #(submit-impl @!opts %)))

(defn start [launcher]
  (when launcher
    (reset! !opts {}))
  (add-tap #'submit)
  (log/info !opts))
