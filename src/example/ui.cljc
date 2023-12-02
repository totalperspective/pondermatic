(ns example.ui
  (:require [pondermatic.core :as p]
            [pondermatic.shell :as sh]
            [pondermatic.flow :as flow]))

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
                 :layout :unauthed}}
    {:id ::authed-layout
     :rule/when {":db/ident" ::state
                 :user {:email '?email}}
     :rule/then {:local/id ::state
                 :layout ::authed}}]))

(def engine (p/->engine "layout" :reset-db? true))

(p/|> engine {:->db rules})
(p/|> engine {:->db data})
