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

(def prn-tap
  (tapper prn))

(defn tap [prefix]
  (tapper
   (fn [x]
     (when prefix
       (tap> (with-meta {prefix x}
               {:portal.viewer/default :portal.viewer/inspector}))))))

(defn run [task]
  (task (fn [x]
          (let [n (or (-> task meta :task) "Success")
                x' (if (fn? x) (x) x)]
            (tap> {n x'})))
        #(throw %)))

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
   (drain flow (-> flow meta :flow)))
  ([flow prefix]
   (drain-using flow (tap prefix))))

(def pairs
  (partial m/reductions
           (fn [[_n-2 n-1] n]
             [n-1 n])
           [nil nil]))

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
       (m/eduction (remove (fn [[n-1 n]] (= n n-1)))
                   (map second))))

(defn split [flow]
  (m/eduction (remove nil?)
              (m/ap (let [items (m/?< flow)]
                      (loop [items items]
                        (when-some [item (first items)]
                          (m/amb item
                                 (recur (rest items)))))))))

(defn <->! [<task !atom]
  (<task (partial reset! !atom)
         #(throw %)))
