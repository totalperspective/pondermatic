(ns pondermatic.web.engine
  (:require [pondermatic.data :as data]
            [pondermatic.pool :as pool]
            [pondermatic.shell :as sh]))

(def worker? (boolean js/globalThis.pondermaticPost))

(defn postWorker [& args]
  (js/globalThis.pondermaticPost (data/write-transit args)))

(defn create-local [type & args]
  {::args args
   ::type type})

(defn contructor [cs type create clone]
  (let [create' (if worker?
                  (apply create-local type)
                  create)
        clone' clone]
    (pool/contructor cs type create' clone')))
