(ns pondermatic.portal)

(defn table [x]
  (if #_{:clj-kondo/ignore [:unresolved-symbol]}
   (instance? clojure.lang.IMeta x)
    (with-meta x {:portal.viewer/default :portal.viewer/table})
    x))
