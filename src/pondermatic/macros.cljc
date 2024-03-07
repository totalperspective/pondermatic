(ns pondermatic.macros
  (:require [clojure.string :as str]
            [missionary.core :as m]))

(defn sym-name [sym]
  (gensym (str (str/replace (name sym) #"^[<>]" "") "-")))

(defmacro |-><
  {:clj-kondo/lint-as 'clojure.core/->}
  [<val & body]
  (let [{:keys [line column file]} (meta &form)
        val (sym-name <val)]
    `(flow/run
      (m/sp (let [~val (m/? ~<val)]
              (-> ~val
                  ~@body)))
      {:line ~line :column ~column :file ~file})))

(defmacro |->><
  {:clj-kondo/lint-as 'clojure.core/->>}
  [<val & body]
  (let [{:keys [line column file]} (meta &form)
        val (sym-name <val)]
    `(flow/run
      (m/sp (let [~val (m/? ~<val)]
              (try
                (->> ~val
                     ~@body))))
      {:line ~line :column ~column :file ~file})))
