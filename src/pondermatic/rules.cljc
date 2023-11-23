(ns pondermatic.rules
  (:require [odoyle.rules :as o]
            [pondermatic.shell :as a :refer [|> |< |<=]]
            [pondermatic.flow :as f]
            [hyperfiddle.rcf :as rcf :refer [tests %]]))

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

(defn ^:export add-rule [rule]
  (list 'add-rule rule))

(defn ^:export insert [id attr->val]
  (list 'insert id attr->val))

(defn ^:export insert* [id:attr->vals]
  (list 'insert* id:attr->vals))

(defn ^:export retract [id attr]
  (list 'retract id attr))

(defn ^:export retract* [id:attrs]
  (list 'retract* id:attrs))

(def ^:export query-all (|<= (map #(o/query-all %))
                             (dedupe)))

(defn ^:export query [rule-name]
  (|<= (map #(o/query-all % rule-name))
       (dedupe)))

(defn process
  [session cmd]
  (tap> {:in ::process
         :cmd (with-meta cmd
                {:portal.viewer/default :portal.viewer/pr-str})})
  (->> session
       (exec cmd)
       o/fire-rules))

(defn ^:export ->session []
  (->> (o/->session)
       (a/engine process)
       a/actor))

(tests
 (let [tap (f/tapper #(do (tap> (with-meta %
                                  {:portal.viewer/default :portal.viewer/table}))
                          (rcf/tap %)))
       session (->session)
       |> (partial |> session)
       rule (o/->rule
             ::character
             {:what
              '[[id ::x x]
                [id ::y y]
                [id ::z z]]
              :when
              (fn [_ {:keys [x y z]}]
                (and (pos? x) (pos? y) (pos? z)))
              :then
              (fn [_ _]
                (println "This will fire twice"))
              :then-finally
              (fn [_]
                (println "This will fire once"))})]

   (rcf/set-timeout! 100)
   (-> session
       (|< (query ::character))
       (f/drain-using tap))
   (|> (add-rule rule))
   % := []
   (|> (insert 1 {::x 3 ::y -1 ::z 0}))

   (|> (insert 2 {::x 10 ::y 2 ::z 1}))
   % := [{:id 2, :x 10, :y 2, :z 1}]

   (|> (insert 2 {::x 10 ::y 2 ::z 1}))
   ;; No change so don't take from the tap

   (|> (insert 3 {::x 7 ::y 1 ::z 2}))
   % := [{:id 2, :x 10, :y 2, :z 1} {:id 3, :x 7, :y 1, :z 2}]

   (|> (insert 3 {::x 7 ::y 1 ::z 2}))
   ;; No change so don't take from the tap

   (|> (insert* [[1 {::x 3 ::y -1 ::z 4}]
                 [2 {::x 10 ::y 2 ::z 5}]
                 [3 {::x 7 ::y 1 ::z 6}]]))
   % := [{:id 2, :x 10, :y 2, :z 5} {:id 3, :x 7, :y 1, :z 6}]

   (|> (retract 1 ::x))
   ;; No change as entity 1 has a negative y co-ordinate
   ;; so don't take from the tap
   (|> (retract* [[1 ::y]
                  [1 ::z]
                  [2 ::x]
                  [2 ::y]
                  [2 ::z]]))
   % := [{:id 3, :x 7, :y 1, :z 6}]

   (|> a/done)))

