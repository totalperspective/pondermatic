(ns pondermatic.portal.utils
  (:require [portal.console :as log]
            [clojure.datafy :as datafy]
            [clojure.walk :as w]))

(defn with-viewer [x viewer]
  (if #?(:clj (instance? clojure.lang.IMeta x) :cljs (implements? cljs.core.IWithMeta x))
    (with-meta x {:portal.viewer/default viewer})
    x))

(defn pprint [x]
  (with-viewer x :portal.viewer/pprint))

(defn tree [x]
  (with-viewer x :portal.viewer/tree))

(defn table [x]
  (with-viewer x :portal.viewer/table))

(defn trace
  ([x]
   (log/trace x)
   x)
  ([x label]
   (log/trace {label x})
   x))

(defn p-trace
  ([x]
   (prn x)
   x)
  ([x label]
   (prn label x)
   x))

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

(defn datafy-value [value]
  (let [{:keys [trace?]} (meta value)]
    (->> value
         (w/postwalk (fn [value]
                       (when trace?
                         (println (exception? value)
                                  value))
                       (cond
                         (exception? value)
                         (error->data value)

                         (fn? value)
                         (pr-str value)

                         :else
                         value)))
         datafy/datafy)))
