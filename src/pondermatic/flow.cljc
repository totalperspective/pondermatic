(ns pondermatic.flow
  (:require [missionary.core :as m]
            [editscript.core :as es]
            [portal.console :as log])
  (:import [missionary Cancelled]))

(def ^:dynamic *dispose-ctx* nil)

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
    ([])
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
     (log/info {(or prefix :tap) x}))))

(defn run
  ([task]
   (run task (-> task meta :task)))
  ([task m]
   (let [dispose! (task (fn [x]
                          (log/trace {::success x ::task m})
                          x)
                        #(log/warn (ex-info "Task Cancelled" {::task m
                                                              ::context *dispose-ctx*} %)))]
     (fn []
       (binding [*dispose-ctx* (ex-info "dispose!" {})]
         (dispose!))))))

(defn counter
  "A reducing function counting the number of items."
  [r _] (inc r))

(defn latest
  ([]
   nil)
  ([p n]
   (or n p)))

(defn drain-using
  ([flow tap]
   (drain-using flow (or (meta tap) {}) tap))
  ([flow m tap]
   (let [dispose! (run (m/reduce tap flow) m)]
     #(try (dispose!)
           (catch Cancelled e
             (log/warn (ex-info "Flow Cancelled" m e)))))))

(defn drain
  ([flow]
   (drain flow (-> flow meta :flow)))
  ([flow prefix]
   (drain-using flow {::flow prefix} (tap prefix))))

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
