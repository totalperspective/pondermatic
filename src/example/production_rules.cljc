(ns example.production-rules
  (:require [pondermatic.core :as p]))

(def data
  (p/kw->qkw
   (p/id->ident
    [{:id "0001",
      :type "donut",
      :name "Cake",
      :ppu 0.55,
      :batters
      [{:id "1001"
        :type "batter"
        :batter "Regular"}
       {:id "1002"
        :type "batter"
        :batter "Chocolate"}
       {:id "1003"
        :type "batter"
        :batter "Blueberry"}
       {:id "1004"
        :type "batter"
        :batter "Devil's Food"}],
      :toppings
      [{:id "5001"
        :type "topping"
        :topping  "None"}
       {:id "5002"
        :type "topping"
        :topping  "Glazed"}
       {:id "5005"
        :type "topping"
        :topping  "Sugar"}
       {:id "5007"
        :type "topping"
        :topping  "Powdered Sugar"}
       {:id "5006"
        :type "topping"
        :topping  "Chocolate with Sprinkles"}
       {:id "5003"
        :type "topping"
        :topping  "Chocolate"}
       {:id "5004"
        :type "topping"
        :topping  "Maple"}]}
     {:id "0002",
      :type "donut"
      :name "Raised",
      :ppu 0.55,
      :batters
      [{:id "1001"
        :type "batter"
        :batter "Regular"}],
      :toppings
      [{:id "5001"
        :type "topping"
        :topping  "None"}
       {:id "5002"
        :type "topping"
        :topping  "Glazed"}
       {:id "5005"
        :type "topping"
        :topping  "Sugar"}
       {:id "5003"
        :type "topping"
        :topping  "Chocolate"}
       {:id "5004"
        :type "topping"
        :topping  "Maple"}]}
     {:id "0003",
      :type "donut",
      :name "Old Fashioned",
      :ppu 0.55,
      :batters
      [{:id "1001"
        :type "batter"
        :batter "Regular"}
       {:id "1002"
        :type "batter"
        :batter "Chocolate"}],
      :toppings
      [{:id "5001"
        :type "topping"
        :topping  "None"}
       {:id "5002"
        :type "topping"
        :topping  "Glazed"}
       {:id "5003"
        :type "topping"
        :topping  "Chocolate"}
       {:id "5004"
        :type "topping"
        :topping  "Maple"}]}])))

(def rules
  (p/ruleset
   [{:id ::combinations
     :rule/name "Combinations"
     :rule/when '{:batters [{":db/ident" ?batter-id
                             :batter ?batter}]
                  :toppings [{":db/ident" ?topping-id
                              :type "topping"
                              :topping ?topping}]}
     :rule/then '{":db/ident" [?batter-id ?topping-id]
                  :type "combination"
                  :batter-id ?batter-id
                  :topping-id ?topping-id}}
    {:id ::regular-glazed-offer
     :rule/name "Regular glazed offer"
     :rule/when '{":db/ident" ?donut-id
                  :type "donut"
                  :name ?name
                  :ppu ?ppu
                  :batters [{":db/ident" ?batter-id
                             :batter "Regular"}]
                  :toppings [{":db/ident" ?topping-id
                              :topping "Glazed"}]}
     :rule/then {:type "offer"
                 :ppu (str '[$ (* 0.7 ?ppu)])
                 :name (str '[$ (str ?name " - Regular Glazed")])
                 :donut-id '?donut-id
                 :batter-id '?batter-id
                 :topping-id '?topping-id}}]))

(def engine (p/->engine "donuts" :reset-db? true))

; (p/|> engine {:})
(p/|> engine {:db/upsert rules})
(p/|> engine {:db/upsert data})
