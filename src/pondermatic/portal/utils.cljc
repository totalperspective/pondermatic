(ns pondermatic.portal.utils
  (:require [portal.console :as log]))

(defn table [x]
  (if #?(:clj (instance? clojure.lang.IMeta x) :cljs (implements? cljs.core.IWithMeta x))
    (with-meta x {:portal.viewer/default :portal.viewer/table})
    x))

(defn trace
  ([x]
   (log/trace x)
   x)
  ([x label]
   (log/trace {label x})
   x))
