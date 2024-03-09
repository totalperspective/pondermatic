(ns scratch.import
  (:require [pondermatic.core :as p]
            [hyperfiddle.rcf :refer [tests %] :as rcf]
            [pondermatic.flow :as flow]
            [portal.console :as log]
            [pondermatic.pool :as pool]
            [clojure.data.json :as json]
            [cognitect.transit :as transit]
            [pondermatic.macros :refer [|-><]]
            [clojure.java.io :as io]))

(require 'user)

(defonce pool (-> {}
                  (pool/contructor :engine p/->engine p/clone>)
                  pool/->pool))

(def |> (partial pool/to-agent! pool))

(defn ->engine [& args]
  (apply pool/add-agent! pool :engine args))

(def with-agent< (partial pool/with-agent< pool))

(def q>< (fn [id & args]
           ((with-agent< p/q><) id args)))

(def model-file "/Users/bahulneel/Projects/Hybr/app/packages/students/nuxt/resources/automated-journey.ponder.json")

(def m-in (json/read-str (slurp model-file) :key-fn keyword))

(def t-in (get-in m-in [0 :transit]))

(def in (io/input-stream (.getBytes t-in)))

(def reader (transit/reader in :json))

(def model (transit/read reader))  ;; => "foo"

(dotimes [n 10]
  (def engine (->engine (str "model-" n) :reset-db? true))

  (|> engine {:!>db {:tx-triples model}})
  (java.lang.Thread/sleep 1000)
  (pool/remove-agent! pool engine))
