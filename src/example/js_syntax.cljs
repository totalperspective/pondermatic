(ns example.js-syntax
  (:require [pondermatic.index :as i]
            [portal.console :as log]
            [pondermatic.portal.utils :as p.util]))

(log/info ::starting)

(def engine (i/create-engine "test" true))

(def q (i/q engine
            (str '[:find ?id ?key ?value
                   :where
                   [?id :data/type :task]
                   [?id ?k ?v]
                   [(str ?k) ?key]
                   [(str ?v) ?value]])
            []
            #(js/console.log %)))

(def rules
  (-> [{"id" "terminate/activate"
        "rule/when" (str '#{{terminate/reason ?reason}})
        "rule/then" (str '{id terminate/task
                           type :task
                           task/active? true
                           task/priority 100})}
       {"id" "other/activate"
        "rule/when" (str '#{{some/var ?val}
                            (> ?val 0)})
        "rule/then" (str '{id other/task
                           type :task
                           task/active? true
                           task/priority 100})}
       {"id" "axiom/before-all"
        "rule/when" (str '#{{id ?task
                             type :task
                             task/active? true
                             (:skip task/priority) ?priority
                             task/before :all}
                            {id ?other-task
                             type :task
                             task/priority ?other-priority
                             task/active? true}
                            (!= ?task ?other-task)
                            (>= ?priority ?other-priority)})
        "rule/then" (str '{id ?task
                           task/priority' [$ (dec ?other-priority) min]})}
       {"id" "axiom/before-task"
        "rule/when" (str '#{{id ?task
                             type :task
                             task/active? true
                             (:skip task/priority) ?priority
                             task/before ?other-task}
                            {id ?other-task
                             type :task
                             task/priority ?other-priority
                             task/active? true}
                            (!= ?task ?other-task)
                            (>= ?priority ?other-priority)})
        "rule/then" (str '{id ?task
                           task/priority' [$ (dec ?other-priority) min]})}]
      clj->js
      i/ruleset))

(def data
  (-> '[{id my/task
         type +:task}
        {id other/task
         type +:task
         task/before [id my/task]}
        {id terminate/task
         type +:task
         task/before +:all}
        {some/var 10}
        {type +:event
         terminate/reason Done}]
      clj->js
      i/dataset))


(i/sh engine #js {"->db" rules})
(i/sh engine #js {"->db" data})

(q)
