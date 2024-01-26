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
  (fn tapper
    ([] nil)
    ([x]
     (tap x)
     x)
    ([_ x]
     (tap x)
     x)))

(defn tap [prefix]
  (tapper
   (fn [x]
     (when prefix
       (tap> (with-meta {prefix x}
               {:portal.viewer/default :portal.viewer/inspector}))))))

(defn run [task]
  (task #(tap> {"Success" %})
        #(tap> %)))

(defn counter [r _] (inc r))    ;; A reducing function counting the number of items.

(defn latest
  ([]
   nil)
  ([p n]
   (or n p)))

(defn drain-using [flow tap]
  (run (m/reduce tap flow)))

(defn drain
  ([flow]
   (drain flow nil))
  ([flow prefix]
   (drain-using flow (tap prefix))))

(def pairs
  (m/reductions (fn [[_ n-1] n]
                  [n-1 n])
                [[] []]))

(defn diff [flow]
  (->> flow
       pairs
       (m/eduction (map (fn [[n-1 n]]
                          {:old (with-meta n-1 {:portal.viewer/default :portal.viewer/table})
                           :new (with-meta n {:portal.viewer/default :portal.viewer/table})
                           :edits (with-meta
                                    (es/get-edits (es/diff n-1 n))
                                    {:portal.viewer/default :portal.viewer/table})})))))

(defn updates [flow]
  (->> flow
       pairs
       (m/eduction (map (fn [[n-1 n]]
                          (when (not= n-1 n)
                            n)))
                   (remove nil?))))

(defn split [flow]
  (m/eduction (remove nil?)
              (m/ap (let [items (m/?> flow)]
                      (loop [items items]
                        (when-some [item (first items)]
                          (m/amb item
                                 (recur (rest items)))))))))
