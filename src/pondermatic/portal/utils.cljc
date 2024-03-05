(ns pondermatic.portal.utils
  (:require [portal.console :as log]
            [clojure.datafy :as datafy]
            [clojure.walk :as w]
            [zuko.io :as z.io]
            [clojure.string :as s]
            [clojure.edn :as edn]
            #?(:cljs [cljs.stacktrace :as st])
            #?(:cljs [cljs.source-map :as sm])))

(def source-map-prefix
  #?(:browser-test :browser-test
     :dev :dev
     :esm :esm
     :node :node
     :npm :npm
     :portal :portal
     :test :test
     :default :dev))

(def source-map (str "." (name source-map-prefix) ".source-map.json"))

(def ^:dynamic sms nil)

#?(:cljs
   (defn with-source-map
     ([thunk]
      (with-source-map source-map thunk))
     ([source-map thunk]
      (binding [sms (->> source-map
                         z.io/slurp
                         edn/read-string)]
        (thunk)))))

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

(defn text [x]
  (with-meta
    [:portal.viewer/markdown x]
    {:portal.viewer/default :portal.viewer/hiccup}))

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

(declare datafy-value)

#?(:cljs
   (defn map-stacktrace [str]
     (let [stack (st/parse-stacktrace {} str {:ua-product :nodejs})]
       (st/mapped-stacktrace-str stack sms))))
#?(:cljs
   (defn error->data [ex]
     (when ex
       (merge
        (when-let [data (or (ex-data ex)
                            (.-data ex))]
          (datafy-value {:data data}))
        {:runtime :cljs
         :cause   (.-message ex)
         :via     (error->data (ex-cause ex))
         :stack   (-> ex
                      .-stack
                      text)})))
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
