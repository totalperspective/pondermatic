(ns pondermatic.flow
  (:require [missionary.core :as m]
            [clojure.string :as str]
            [editscript.core :as es]))

(defn crash [e]                                ;; let it crash philosophy
  (println e)
  (println (.-stack e))
  #?(:cljs
     (.exit js/process -1)
     :clj
     (System/exit -1)))

(defn tap [prefix]
  (fn [_ x]
    (when prefix
      (tap> (with-meta {prefix x}
              {:portal.viewer/default :portal.viewer/inspector})))
    x))

(defn run [task]
  (task #(tap> {"Success" %})
        #(tap> {"Error" %
                :info (str/split-lines (.-stack %))})))

(defn counter [r _] (inc r))    ;; A reducing function counting the number of items.

(defn latest [p n] (or n p))

(defn drain
  ([flow]
   (drain flow nil))
  ([flow prefix]
   (run (m/reduce (tap prefix) flow))))

(defn diff [flow]
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
