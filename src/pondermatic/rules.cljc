(ns pondermatic.rules
  (:require [odoyle.rules :as o]
            [pondermatic.actor :as a :refer [|> |<]]
            [missionary.core :as m]
            [pondermatic.flow :as f]))

(defn cmd-type
  [[cmd] _]
  cmd)

(defmulti exec cmd-type)

(defmethod exec 'add-rule
  [[_ rule] session]
  (o/add-rule session rule))

(defmethod exec 'insert
  [[_ id attr->val] session]
  (o/insert session id attr->val))

(defmethod exec 'insert*
  [[_ id:attr->vals] session]
  (reduce (fn [session [id attr->val]]
            (o/insert session id attr->val))
          session
          id:attr->vals))

(defmethod exec 'retract
  [[_ id attr] session]
  (o/retract session id attr))

(defmethod exec 'retract*
  [[_ id:attrs] session]
  (reduce (fn [session [id attr]]
            (o/retract session id attr))
          session
          id:attrs))

(defn add-rule [rule]
  (list 'add-rule rule))

(defn insert [id attr->val]
  (list 'insert id attr->val))

(defn insert* [id:attr->vals]
  (list 'insert* id:attr->vals))

(defn retract [id attr]
  (list 'retract id attr))

(defn retract* [id:attrs]
  (list 'retract* id:attrs))

(defn query-all [<session]
  (m/eduction (map #(o/query-all %)) (dedupe) <session))

(defn query [rule-name]
  (fn [<session]
    (m/eduction (map #(o/query-all % rule-name)) <session)))

(defn engine
  [session]
  (fn [return cmd]
    (->> session
         (exec cmd)
         o/fire-rules
         return
         engine)))

(def session (-> (o/->session)
                 engine
                 a/actor))

(defn run-test []
  (let [rule (o/->rule
              ::character
              {:what
               '[[id ::x x]
                 [id ::y y]
                 [id ::z z]]
               :when
               (fn [session {:keys [x y z] :as match}]
                 (and (pos? x) (pos? y) (pos? z)))
               :then
               (fn [session match]
                 (println "This will fire twice"))
               :then-finally
               (fn [session]
                 (println "This will fire once"))})]

    (f/drain ::all (f/diff (|< session query-all)))

    (-> session
        (|> (add-rule rule))
        (|> (insert 1 {::x 3 ::y -1 ::z 0}))
        (|> (insert 2 {::x 10 ::y 2 ::z 1}))
        (|> (insert 3 {::x 7 ::y 1 ::z 2}))
        (|> (insert 3 {::x 7 ::y 1 ::z 2}))
        (|> (insert* [[1 {::x 3 ::y -1 ::z 4}]
                      [2 {::x 10 ::y 2 ::z 5}]
                      [3 {::x 7 ::y 1 ::z 6}]]))
        (|> (retract 1 ::x))
        (|> (retract* [[1 ::y]
                       [1 ::z]
                       [2 ::x]
                       [2 ::y]
                       [2 ::z]]))
        (|> a/done))))
