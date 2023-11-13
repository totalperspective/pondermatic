(ns pondermatic.rules
  (:require [odoyle.rules :as o]
            [missionary.core :as m]
            [pondermatic.flow :as f]))

(defn actor
  ([init] (actor init f/crash))
  ([init fail]
   (let [self (m/mbx)
         >return (m/stream
                  (m/eduction
                   (remove nil?)
                   (m/ap
                    (loop [process init]
                      (let [cmd (m/? self)]
                        (when (not= ::done cmd)
                          (let [emit (m/rdv)
                                next (process emit cmd)]
                            (m/amb
                             (m/? emit)
                             (recur next)))))))))]
     (f/drain nil >return)
     {:actor self
      :return >return})))

(defn cmd-type
  [[cmd] _]
  cmd)

(defmulti exec cmd-type)

(def session
  (let [proc (fn proc [session]
               (fn [emit cmd]
                 (let [session' (o/fire-rules (exec cmd session))
                       run (m/sp (m/? (emit session')))]
                   (run identity f/crash)
                   (proc session'))))]
    (actor (proc (o/->session)))))

(def |< (:return session))
(def |> (:actor session))

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

(defn query-all []
  (m/eduction (map #(o/query-all %)) |<))

(defn query [rule-name]
  (m/eduction (map #(o/query-all % rule-name)) |<))

(defn run-test []
  (let [rule (o/->rule
              ::character
              {:what
               '[[id ::x x]
                 [id ::y y]]
               :when
               (fn [session {:keys [x y] :as match}]
                 (and (pos? x) (pos? y)))
               :then
               (fn [session match]
                 (println "This will fire twice"))
               :then-finally
               (fn [session]
                 (println "This will fire once"))})]

    (f/drain ::all (query ::character))
    (|> (add-rule rule))
    (|> (insert 1 {::x 3 ::y -1}))
    (|> (insert 2 {::x 10 ::y 2}))
    (|> (insert 3 {::x 7 ::y 1}))
    (|> (insert 3 {::x 7 ::y 1}))
    (|> (insert* [[1 {::x 3 ::y -1}]
                  [2 {::x 10 ::y 2}]
                  [3 {::x 7 ::y 1}]]))
    (|> (retract 1 ::x))
    (|> (retract* [[1 ::y]
                   [2 ::x]
                   [2 ::y]]))))
