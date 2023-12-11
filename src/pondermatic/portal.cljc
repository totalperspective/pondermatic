(ns pondermatic.portal
  (:require [portal.api :as p]
            ;; #?(:cljs [portal.web :as pw])
            [clojure.datafy :as datafy]))

(def submit (comp p/submit datafy/datafy))

(defn table [x]
  (if #_{:clj-kondo/ignore [:unresolved-symbol]}
   (instance? #?(:clj clojure.lang.IMeta :cljs cljs.core.IMeta) x)
    (with-meta x {:portal.viewer/default :portal.viewer/table})
    x))

; Add portal as a tap> target
(defn start [launcher]
  (case launcher
    nil (p/open)
    ;; :browser (pw/open)
    (p/open {:launcher launcher}))
  (add-tap #'submit)
  p/close)

(defn trace
  ([x]
   (tap> x)
   x)
  ([x label]
   (tap> {label x})
   x))
