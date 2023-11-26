(ns example.production-rules
  (:require [pondermatic.core :as p]))

(def data
  (p/id->ident
   [{:id "0001",
     :type "donut",
     :name "Cake",
     :ppu 0.55,
     :batters
     {:batter
      [{:id "1001", :type "Regular"}
       {:id "1002", :type "Chocolate"}
       {:id "1003", :type "Blueberry"}
       {:id "1004", :type "Devil's Food"}]},
     :topping
     [{:id "5001", :type "None"}
      {:id "5002", :type "Glazed"}
      {:id "5005", :type "Sugar"}
      {:id "5007", :type "Powdered Sugar"}
      {:id "5006", :type "Chocolate with Sprinkles"}
      {:id "5003", :type "Chocolate"}
      {:id "5004", :type "Maple"}]}
    {:id "0002",
     :type "donut",
     :name "Raised",
     :ppu 0.55,
     :batters {:batter [{:id "1001", :type "Regular"}]},
     :topping
     [{:id "5001", :type "None"}
      {:id "5002", :type "Glazed"}
      {:id "5005", :type "Sugar"}
      {:id "5003", :type "Chocolate"}
      {:id "5004", :type "Maple"}]}
    {:id "0003",
     :type "donut",
     :name "Old Fashioned",
     :ppu 0.55,
     :batters
     {:batter
      [{:id "1001", :type "Regular"} {:id "1002", :type "Chocolate"}]},
     :topping
     [{:id "5001", :type "None"}
      {:id "5002", :type "Glazed"}
      {:id "5003", :type "Chocolate"}
      {:id "5004", :type "Maple"}]}]))

(def rules
  (p/id->ident
   '[{:id ::regular-glazed-offer
      :name "Regular glazed offer"
      :type p/rule-type
      :when {:id ?donut-id
             :type "donut"
             :name ?name
             :ppu ?ppu
             :batters {:batter {:id ?batter-id :type "Regular"}}
             :topping {:id ?topping-id :type "Glazed"}}
      :then {:type "offer"
             :ppu [$ "(* 0.7 ?ppu)"]
             :name [$ "(str ?name \" - Regular Glazed\")"]
             :donut {:id ?donut-id}
             :batter {:id ?batter-id}
             :topping {:id ?topping-id}}}]))

(def engine (p/->engine "donuts"))

; (p/|> engine {:})
(p/|> engine {:db/upsert rules})
