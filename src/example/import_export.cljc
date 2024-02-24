(ns example.import-export
  (:require [pondermatic.core :as p]
            [pondermatic.flow :as flow]
            [pondermatic.shell :as sh :refer [|>]]
            [portal.console :as log])
  #?(:cljs
     (:require-macros [pondermatic.macros :refer [|->]])
     :default
     (:require [pondermatic.macros :refer [|->]])))

(def data
  (p/dataset
   [{:id "red-apple"
     :fruit "Apple"
     :size "Large"
     :color "Red"}]))

(def data'
  (p/dataset
   [{:id "green-apple"
     :fruit "Apple"
     :size "Small"
     :color "Green"}]))

(def rules
  (p/ruleset
   [{:id ::fetch-apples
     :rule/when '{:fruit "Apple"
                  & ?id}
     :rule/then '{:id :apples
                  :apple+ ?id}}]))

(defn <>apples [engine]
  (p/q>< engine
         '[:find ?apple ?size ?color
           :where
           [?id :db/ident :apples]
           [?id :data/apple ?apples]
           [?apples :a/contains ?apple-id]
           [?apple-id :db/ident ?apple]
           [?apple-id :data/color ?color]
           [?apple-id :data/size ?size]]))

(def !data (atom nil))


(let  [engine (p/->engine "fruit" :reset-db? true)
       <>q (with-meta (<>apples engine) {:flow :query1})]
  (|-> <>q
       (flow/drain-using {::flow :fruit} flow/prn-tap))
  (-> engine
      (|> {:->db rules})
      (|> {:->db data})
      (p/export->! !data)
      sh/stop))

(log/info "Import")

(let [engine (p/->engine "fruit2" :reset-db? true)
      <>q (<>apples engine)]
  (|-> <>q
       (flow/drain-using {::flow :fruit2} flow/prn-tap))
  (-> engine
      (p/import @!data)
      (|> {:->db data'})
      sh/stop))
