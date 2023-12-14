(ns pondermatic.portal.utils)

(defn table [x]
  (if #?(:clj (instance? clojure.lang.IMeta x) :cljs (implements? cljs.core.IWithMeta x))
    (with-meta x {:portal.viewer/default :portal.viewer/table})
    x))


(defn trace
  ([x]
   (tap> x)
   x)
  ([x label]
   (tap> {label x})
   x))
