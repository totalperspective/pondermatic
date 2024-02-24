(ns example.ui
  (:require [pondermatic.core :as p]
            [clojure.pprint :as cpp]
            [pondermatic.shell :refer [|>]]))

(def data
  (p/dataset
   [{:id ::state
     :user {:email "example@example.com"
            :name "Test User"}}
    {:id :unauthed
     :type :layout
     :content {:type :slot}}
    {:id :authed
     :type :layout
     :header {:type :slot}
     :menu {:type :slot}
     :content {:type :slot}
     :footer {:type :slot}}]))

(def rules
  (p/ruleset
   [{:id ::unauthed-layout
     :rule/when {":db/ident" ::state
                 :user nil}
     :rule/then {:local/id ::state
                 :layout :unauthed
                 :page :none}}
    {:id ::authed-layout
     :rule/when {":db/ident" ::state
                 :user {:email '?email}}
     :rule/then {:local/id ::state
                 :layout ::authed
                 :page :none}}]))

(cpp/pprint rules)
(def engine (p/->engine "layout" :reset-db? true))

(-> engine
    (|> {:->db rules})
    (|> {:->db data})
    p/stop)
