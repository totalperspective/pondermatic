(ns pondermatic.portal)

(defn table [x]
  (if #_{:clj-kondo/ignore [:unresolved-symbol]}
   (instance? #?(:clj clojure.lang.IMeta :cljs cljs.core.IMeta) x)
    (with-meta x {:portal.viewer/default :portal.viewer/table})
    x))
