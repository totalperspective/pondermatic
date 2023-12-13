(ns example.syntax
  (:require [pondermatic.index :as i]
            [pondermatic.portal :as pp]
            [pondermatic.portal.utils :as ppu]))

(defonce _ (pp/start :vs-code))

(def rules
  (-> [{"id" "terminate/activate"
        "rule/when" (str '#{{terminate/reason ?reason}})
        "rule/then" (str '{id terminate/task
                           task/active? true
                           task/priority 100})}
       {"id" "other/task"
        "rule/when" (str '#{{some/var ?val}
                            (> ?val 0)})
        "rule/then" (str '{id terminate/task
                           task/active? true
                           task/priority 100})}
       {"id" "before-all/rule"
        "rule/when" (str '#{{id ?task
                             task/active? true
                             task/priority ?priority
                             before *}
                            {id ?other-task
                             task/active? true}
                            (!= ?task ?other-task)})
        "rule/then" (str '{id ?other-task
                           task/priority "[$ (dec ?priority)]"})}]
      clj->js
      (ppu/trace :rule/js)
      i/ruleset))

(def data
  (-> '[{some/var 10}
        {terminate/reason "Done"}]
      clj->js
      (ppu/trace :data/js)
      i/dataset))

(def engine (i/create-engine "test" true))

(i/sh engine #js {"->db" rules})
(i/sh engine #js {"->db" data})
