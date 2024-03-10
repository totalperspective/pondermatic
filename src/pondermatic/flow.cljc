(ns pondermatic.flow
  (:require [missionary.core :as m]
            [editscript.core :as es]
            [portal.console :as log]
            [pondermatic.data :as data])
  (:import [missionary Cancelled]))

(def ^:dynamic *dispose-ctx* nil)

(def ^:dynamic *tap-print* false)

(defn crash [e]                                ;; let it crash philosophy
  (println e)
  (println (.-stack e))
  #?(:cljs
     (.exit js/process -1)
     :clj
     (System/exit -1)))

(defn return [emit result]
  (let [run (m/sp (m/? (emit result)))]
    (run identity crash)
    result))

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

(defn printer [& args]
  (when *tap-print*
    (apply prn args)))

(def prn-tap
  (tapper printer))

(defn tap
  ([prefix]
   (tap prefix identity))
  ([prefix format]
   (tapper
    (fn [x]
      (when (and prefix x)
        #?(:cljs
           (when *tap-print*
             (js/console.debug (str prefix) (pr-str (format x))))
           :default
           (printer prefix x)))))))

(defn run
  ([task]
   (run task (-> task meta :task)))
  ([task m]
   (let [dispose! (task (fn [x]
                          ;; (log/trace {::success x ::task m})
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
     (fn []
       (binding [*dispose-ctx* (ex-info "dispose!" {::drain-usng m})]
         (dispose!))))))

(defn drain
  ([flow]
   (drain flow (-> flow meta :flow)))
  ([flow prefix]
   (drain-using flow {::flow prefix} (tap prefix keys))))

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

;; (defn split [flow]
;;   (m/eduction (remove nil?)
;;               (m/ap (let [items (m/?< flow)]
;;                       (loop [items items]
;;                         (when-some [item (first items)]
;;                           (m/amb item
;;                                  (recur (rest items)))))))))

(defn <->! [<task !atom]
  (<task (partial reset! !atom)
         #(throw %)))

(defn await-promise
  "Returns a task completing with the result of given promise"
  [p]
  (let [v (m/dfv)]
    (.then p #(v (fn [] %)) #(v (fn [] (throw %))))
    (m/absolve v)))

(defn mbx> [<m & [prefix]]
  (let [>flow (m/seed (repeat <m))]
    (m/stream
     (m/ap (let [<m (m/?> >flow)
                 v (m/? <m)]
             (printer prefix v)
             v)))))
