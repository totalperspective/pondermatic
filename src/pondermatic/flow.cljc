(ns pondermatic.flow
  (:require [missionary.core :as m]
            [editscript.core :as es]))

(defn crash [e]                                ;; let it crash philosophy
  (println e)
  (println (.-stack e))
  #?(:cljs
     (.exit js/process -1)
     :clj
     (System/exit -1)))

(defn tapper
  [tap]
  (fn
    ([] nil)
    ([_ x]
     (tap x)
     x)))

(defn ^:export tap [prefix]
  (tapper
   (fn [x _]
     (when prefix
       (tap> (with-meta {prefix x}
               {:portal.viewer/default :portal.viewer/inspector}))))))

(defn ^:export run [task]
  (task #(tap> {"Success" %})
        #(tap> {"Error" %})))

(defn ^:export counter [r _] (inc r))    ;; A reducing function counting the number of items.

(defn ^:export latest [p n] (or n p))

(defn ^:export drain-using [flow tap]
  (run (m/reduce tap flow)))

(defn ^:export drain
  ([flow]
   (drain flow nil))
  ([flow prefix]
   (drain-using flow (tap prefix))))

(defn ^:export diff [flow]
  (->> flow
       (m/reductions (fn [[_ n-1] n]
                       [n-1 n])
                     [[] []])
       (m/eduction (map (fn [[n-1 n]]
                          {:old (with-meta n-1 {:portal.viewer/default :portal.viewer/table})
                           :new (with-meta n {:portal.viewer/default :portal.viewer/table})
                           :edits (with-meta
                                    (es/get-edits (es/diff n-1 n))
                                    {:portal.viewer/default :portal.viewer/table})})))))
