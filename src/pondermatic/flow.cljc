(ns pondermatic.flow
  (:require [missionary.core :as m]
            [clojure.string :as str]))

(defn crash [e]                                ;; let it crash philosophy
  (println e)
  (println (.-stack e))
  (.exit js/process -1))

(defn tap [prefix]
  (fn [_ x]
    (when prefix
      (tap> {prefix x}))
    x))

(defn run [task]
  (task #(tap> {"Success" %})
        #(tap> {"Error" %
                :info (str/split-lines (.-stack %))})))

(defn counter [r _] (inc r))    ;; A reducing function counting the number of items.

(defn latest [p n] (or n p))

(defn drain [prefix flow]
  (run (m/reduce (tap prefix) flow)))
