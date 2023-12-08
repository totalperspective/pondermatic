(ns pondermatic.portal
  (:require [portal.api :as p]
            [clojure.datafy :as datafy]))

(def submit (comp p/submit datafy/datafy))

(defn table [x]
  (if #_{:clj-kondo/ignore [:unresolved-symbol]}
   (instance? #?(:clj clojure.lang.IMeta :cljs cljs.core.IMeta) x)
    (with-meta x {:portal.viewer/default :portal.viewer/table})
    x))

(defn start [launcher]
  (let [p (p/open {:launcher launcher})]
    (add-tap #'submit)
    p/close)) ; Add portal as a tap> target)

(defn trace
  ([x]
   (tap> x)
   x)
  ([x label]
   (tap> {label x})
   x))
