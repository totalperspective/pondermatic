(ns pondermatic.portal.utils
  (:require [portal.console :as log]))

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
