(ns pondermatic.rules
  (:require [odoyle.rules :as o]
            [pondermatic.shell :as sh :refer [|> |< |<=]]
            [pondermatic.flow :as f]
            [pondermatic.portal.utils :as p.utils]
            [hyperfiddle.rcf :as rcf :refer [tests %]]
            [missionary.core :as m]
            [taoensso.tufte :as tufte :refer [p]])
  #?(:cljs
     (:require-macros [portal.console :as log])
     :default
     (:require [portal.console :as log])))

(defn cmd-type
  [[cmd] _]
  cmd)

(defmulti exec cmd-type)

(defn upsert-rule [session rule]
  (p ::upsert-rule
     (try
       (o/add-rule session rule)
       (catch #?(:clj Exception :cljs js/Error) _e
      ;; (log/warn e)
         (-> session
             (o/remove-rule (:name rule))
             (o/add-rule rule))))))

(defmethod exec 'add-rule
  [[_ rule] session]
  (upsert-rule session rule))

(defmethod exec 'add-rules
  [[_ rules] session]
  (reduce upsert-rule session rules))

(defmethod exec 'insert
  [[_ id attr->val] session]
  (o/insert session id attr->val))

(defmethod exec 'insert*
  [[_ id:attr->vals] session]
  ;; (tap> (p/table id:attr->vals))
  (reduce (fn [session data]
            ;; (tap> (p/table data))
            (apply o/insert session data))
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

(defmethod exec 'noop
  [[_ _] session]
  (log/trace {::noop session})
  session)

(defn add-rule [rule]
  (list 'add-rule rule))

(defn add-rules [rules]
  (list 'add-rules rules))

(defn insert [id attr->val]
  (list 'insert id attr->val))

(defn insert* [id:attr->vals]
  (list 'insert* id:attr->vals))

(defn retract [id attr]
  (list 'retract id attr))

(defn retract* [id:attrs]
  (list 'retract* id:attrs))

(defn noop []
  (list 'noop))

(def query-all< (|<= (map #(o/query-all %))
                     (dedupe)))

(defn query< [rule-name]
  (|<= (map #(o/query-all % rule-name))
       (dedupe)))

(defn process
  [session cmd]
  (p ::process
     (when-not (= cmd sh/done)
       (log/debug {::cmd cmd})
       (vary-meta
        (->> session
             (exec cmd)
             o/fire-rules)
        merge
        {::sh/safe-keys [:last-id]}))))

(defn ->session []
  (->> (o/->session)
       (sh/engine process)
       (sh/actor ::prefix)))

(defn clone> [session]
  (m/sp
   (->> identity
        (sh/|!> session)
        m/?
        (sh/engine process)
        (sh/actor ::prefix))))

(tests
 (let [tap (f/tapper #(do (log/trace (p.utils/table %))
                          (rcf/tap %)))
       session (->session)
       |s> (partial |> session)
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
                (println "This will fire once"))})
       dispose! (-> session
                    (|< (query< ::character))
                    (f/drain-using {::flow :query ::query ::character} tap))]
   (|s> (add-rule rule))
   (|s> (insert 1 {::x 3 ::y -1 ::z 0}))

   (|s> (insert 2 {::x 10 ::y 2 ::z 1}))

   (|s> (insert 2 {::x 10 ::y 2 ::z 1}))

   (|s> (insert 3 {::x 7 ::y 1 ::z 2}))

   (|s> (insert 3 {::x 7 ::y 1 ::z 2}))

   (|s> (insert* [[1 {::x 3 ::y -1 ::z 4}]
                  [2 {::x 10 ::y 2 ::z 5}]
                  [3 {::x 7 ::y 1 ::z 6}]]))

   (|s> (retract 1 ::x))
   (|s> (retract* [[1 ::y]
                   [1 ::z]
                   [2 ::x]
                   [2 ::y]
                   [2 ::z]]))
   % := []
   % := [{:id 2, :x 10, :y 2, :z 1}]
   % := [{:id 2, :x 10, :y 2, :z 1} {:id 3, :x 7, :y 1, :z 2}]
   % := [{:id 2, :x 10, :y 2, :z 5} {:id 3, :x 7, :y 1, :z 6}]
   % := [{:id 3, :x 7, :y 1, :z 6}]

   (dispose!)
   (sh/stop session)))
