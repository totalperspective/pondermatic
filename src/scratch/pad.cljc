(ns scratch.pad)

#_{:clj-kondo/ignore [:unresolved-namespace]}
(clojure.pprint/pprint
 '(nil :tap {:worker
             {:ns pondermatic.flow, :time #inst "2024-03-05T18:36:31.324-00:00", :file "/Users/bahulneel/Projects/TotalPerspective/pondermatic/src/pondermatic/flow.cljc", :column 6, :level :info, :line 34, :result
              {:pondermatic.shell/>return
               {:pondermatic.pool/contructors
                {:db
                 {:create "#object[pondermatic$db$__GT_conn]", :clone "#object[pondermatic$db$clone_GT_]"}, :rules
                 {:create "#object[pondermatic$rules$__GT_session]", :clone "#object[pondermatic$rules$clone_GT_]"}, :engine
                 {:create "#object[pondermatic$core$__GT_engine]", :clone "#object[pondermatic$engine$conn_GT_]"}}
                , :agents {}, :pondermatic.pool/agents
                {#uuid "6fe3597c-1443-49e5-ac95-e2105b550bf0"
                 {:agent {:pondermatic.shell/send #object[missionary.impl.Mailbox.Port], :pondermatic.shell/receive #object[missionary.impl.Propagator.Publisher]}, :clone "#object[pondermatic$engine$conn_GT_]"}}}}, :runtime :cljs, :form {(or prefix :tap) x}}}))
