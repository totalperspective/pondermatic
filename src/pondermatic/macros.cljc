(ns pondermatic.macros
  (:require [clojure.string :as str]
            [missionary.core :as m]))

(defn sym-name [sym]
  (gensym (str (str/replace (name sym) #"^[<>]" "") "-")))

(defmacro |->
  {:clj-kondo/lint-as 'clojure.core/->}
  [<val & body]
  (let [val (sym-name <val)]
    `(flow/run
      (m/sp (let [~val (m/? ~<val)]
              (-> ~val
                  ~@body))))))

(defmacro |->>
  {:clj-kondo/lint-as 'clojure.core/->>}
  [<val & body]
  (let [val (sym-name <val)]
    `(flow/run
      (m/sp (let [~val (m/? ~<val)]
              (->> ~val
                   ~@body))))))
