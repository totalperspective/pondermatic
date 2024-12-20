(ns pondermatic.portal.utils
  (:require [portal.console :as log]
            [clojure.datafy :as datafy]
            [clojure.walk :as w]))

(defn with-viewer [x viewer]
  (if #?(:clj (instance? clojure.lang.IMeta x) :cljs (implements? cljs.core.IWithMeta x))
    (with-meta x {:portal.viewer/default viewer})
    x))

(defn log [x]
  (with-viewer x :portal.viewer/log))

(defn pprint [x]
  (with-viewer x :portal.viewer/pprint))

(defn tree [x]
  (with-viewer x :portal.viewer/tree))

(defn table [x]
  (with-viewer x :portal.viewer/table))

(defn text [x]
  (with-meta
    [:portal.viewer/markdown (str "```\n" x "\n```")]
    {:portal.viewer/default :portal.viewer/hiccup}))

(defn trace
  ([x]
   (log/trace x)
   x)
  ([x label]
   (log/trace {label x})
   x))

(declare datafy-value)

#?(:cljs
   (defn error->data [ex]
     (when ex
       (js/console.error ex)
       (merge
        (when-let [data (or (ex-data ex)
                            (.-data ex))]
          (datafy-value {:data data}))
        (let [stack (.-stack ex)]
          {:runtime :cljs
           :cause   (.-message ex)
           :via     (error->data (ex-cause ex))
           :stack   (text stack)}))))
   :default
   (defn error->data [ex]
     (assoc (datafy/datafy ex) :runtime :clj)))

(defn exception? [e]
  (instance? #?(:cljs js/Error :default Exception) e))

(def datafy-value
  (memoize (fn datafy-value [value]
             (let [{:keys [trace?]} (meta value)]
               (->> value
                    (w/postwalk (fn datafy-value [value]
                                  (when trace?
                                    (println (exception? value)
                                             value))
                                  (cond
                                    (exception? value)
                                    (error->data value)

                                    (fn? value)
                                    (pr-str value)

                                    (re-find #"^#object" (pr-str value))
                                    (pr-str value)

                                    :else
                                    value)))
                    datafy/datafy)))))