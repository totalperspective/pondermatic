(ns pondermatic.flow
  (:require [missionary.core :as m]
            [clojure.string :as str]
            [clojure.set :refer [difference]]
            [editscript.core :as es]))

(defn crash [e]                                ;; let it crash philosophy
  (println e)
  (println (.-stack e))
  (.exit js/process -1))

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

(defn drain [prefix flow]
  (run (m/reduce (tap prefix) flow)))

(defn diff [flow]
  (m/eduction (map (fn [[n-1 n]]
                     {:old n-1
                      :new n
                      :edits (es/get-edits (es/diff n-1 n))}))
              (m/reductions (fn [[_ n-1] n]
                              [n-1 n])
                            [[] []]
                            flow
                            #_(m/eduction (map (partial apply sorted-set)) flow))))
