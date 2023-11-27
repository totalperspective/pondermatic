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
      :type "donut"
      :name "Raised",
      :ppu 0.55,
      :batters
      {:batter
       [{:id "1001", :type "Regular"}]},
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
       [{:id "1001", :type "Regular"}
        {:id "1002", :type "Chocolate"}]},
      :topping
      [{:id "5001", :type "None"}
       {:id "5002", :type "Glazed"}
       {:id "5003", :type "Chocolate"}
       {:id "5004", :type "Maple"}]}])))

(def rules
  (p/ruleset
   [{:db/ident ::regular-glazed-offer
     :rule/name "Regular glazed offer"
     :rule/when '{:type "donut"
                  :name ?name
                  :ppu ?ppu
                  :batters {:batter [{:type "Chocolate"}]}
                  :topping [{:type "Maple"}]}
     :rule/then {:type "offer"
                 :ppu (str '[$ (* 0.7 ?ppu)])
                 :name (str '[$ (str ?name \" - Regular Glazed \")])
                 :donut '{:id ?donut-id}
                 :batter '{:id ?batter-id}
                 :topping '{:id ?topping-id}}}]))
(prn rules)
(def engine (p/->engine "donuts" :reset-db? true))

; (p/|> engine {:})
(p/|> engine {:db/upsert rules})
(p/|> engine {:db/upsert data})
