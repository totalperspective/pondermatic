(ns scratch.pad)

(clojure.pprint/pprint
 '{:ns pondermatic.eval,
   :time #inst "2024-02-24T20:59:33.742-00:00",
   :file "pondermatic/eval.cljc",
   :column 10, :level :error, :line 84, :result
   {:via
    [{:type clojure.lang.ExceptionInfo, :message "Could not resolve symbol: ee7b84b6", :data {:eval/expr "clojure.lang.LazySeq@ee7b84b6", :eval/bindings "nil"}, :at [pondermatic.eval$eval_string$fn__40595 invoke "eval.cljc" 84]}
     {:type clojure.lang.ExceptionInfo, :message "Could not resolve symbol: ee7b84b6", :data {:type :sci/error, :line 1, :column 21, :message "Could not resolve symbol: ee7b84b6", :sci.impl/callstack #object[clojure.lang.Volatile 0xa684843 {:status :ready, :val ({:line 1, :column 21, :ns #object[sci.lang.Namespace 0x28c62104 "user"], :file nil} {:line 1, :column 21, :ns #object[sci.lang.Namespace 0x28c62104 "user"], :file nil, :sci.impl/f-meta {:name deref, :doc "Also reader macro: @ref/@agent/@var/@atom/@delay/@future/@promise. Within a transaction,\n  returns the in-transaction-value of ref, else returns the\n  most-recently-committed value of ref. When applied to a var, agent\n  or atom, returns its current state. When applied to a delay, forces\n  it if not already forced. When applied to a future, will block if\n  computation not complete. When applied to a promise, will block\n  until a value is delivered.  The variant taking a timeout can be\n  used for blocking references (futures and promises), and will return\n  timeout-val if the timeout (in milliseconds) is reached before a\n  value is available. See also - realized?.", :arglists ([ref] [ref timeout-ms timeout-val]), :sci/built-in true, :ns #object[sci.lang.Namespace 0x408edc01 "clojure.core"]}})}], :file nil, :phase "analysis"}, :at [sci.impl.utils$rethrow_with_location_of_node invokeStatic "utils.cljc" 142]} {:type clojure.lang.ExceptionInfo, :message "Could not resolve symbol: ee7b84b6", :data {:type :sci/error, :line 1, :column 21, :file nil, :phase "analysis"}, :at [sci.impl.utils$throw_error_with_location invokeStatic "utils.cljc" 41]}], :trace [[sci.impl.utils$throw_error_with_location invokeStatic "utils.cljc" 41] [sci.impl.utils$throw_error_with_location invoke "utils.cljc" 36] [sci.impl.resolve$throw_error_with_location invokeStatic "resolve.cljc" 12] [sci.impl.resolve$throw_error_with_location invoke "resolve.cljc" 11] [sci.impl.resolve$resolve_symbol invokeStatic "resolve.cljc" 267] [sci.impl.resolve$resolve_symbol invoke "resolve.cljc" 259] [sci.impl.analyzer$analyze invokeStatic "analyzer.cljc" 1790] [sci.impl.analyzer$analyze invoke "analyzer.cljc" 1783] [sci.impl.analyzer$analyze invokeStatic "analyzer.cljc" 1785] [sci.impl.analyzer$analyze invoke "analyzer.cljc" 1783] [sci.impl.analyzer$analyze_children$fn__34391 invoke "analyzer.cljc" 308] [clojure.core$mapv$fn__8535 invoke "core.clj" 6979] [clojure.lang.PersistentList reduce "PersistentList.java" 141] [clojure.core$reduce invokeStatic "core.clj" 6885] [clojure.core$mapv invokeStatic "core.clj" 6970] [clojure.core$mapv invoke "core.clj" 6970] [sci.impl.analyzer$analyze_children invokeStatic "analyzer.cljc" 308] [sci.impl.analyzer$analyze_children invoke "analyzer.cljc" 307] [sci.impl.analyzer$analyze_call invokeStatic "analyzer.cljc" 1626] [sci.impl.analyzer$analyze_call invoke "analyzer.cljc" 1470] [sci.impl.analyzer$analyze invokeStatic "analyzer.cljc" 1821] [sci.impl.analyzer$analyze invoke "analyzer.cljc" 1783] [sci.impl.interpreter$eval_form invokeStatic "interpreter.cljc" 34] [sci.impl.interpreter$eval_form invoke "interpreter.cljc" 15] [sci.impl.interpreter$eval_string_STAR_ invokeStatic "interpreter.cljc" 70] [sci.impl.interpreter$eval_string_STAR_ invoke "interpreter.cljc" 57] [sci.impl.interpreter$eval_string_STAR_ invokeStatic "interpreter.cljc" 59] [sci.impl.interpreter$eval_string_STAR_ invoke "interpreter.cljc" 57] [sci.impl.interpreter$eval_string invokeStatic "interpreter.cljc" 81] [sci.impl.interpreter$eval_string invoke "interpreter.cljc" 77] [sci.core$eval_string invokeStatic "core.cljc" 248] [sci.core$eval_string invoke "core.cljc" 225] [pondermatic.eval$eval_string invokeStatic "eval.cljc" 82] [pondermatic.eval$eval_string invoke "eval.cljc" 72] [pondermatic.eval$eval_string invokeStatic "eval.cljc" 74] [pondermatic.eval$eval_string invoke "eval.cljc" 72] [clojure.core$map$fn__5935 invoke "core.clj" 2770] [clojure.lang.LazySeq sval "LazySeq.java" 42] [clojure.lang.LazySeq seq "LazySeq.java" 51] [clojure.lang.RT seq "RT.java" 535] [clojure.core$seq__5467 invokeStatic "core.clj" 139] [clojure.core$every_QMARK_ invokeStatic "core.clj" 2696] [clojure.core$every_QMARK_ invoke "core.clj" 2689] [pondermatic.engine$add_base_rules$pondermatic_engine_rules__42415$when_fn__42425 invoke "engine.cljc" 179] [odoyle.rules$left_activate_memory_node$fn__24642 invoke "rules.cljc" 372] [odoyle.rules$left_activate_memory_node invokeStatic "rules.cljc" 370] [odoyle.rules$left_activate_memory_node invoke "rules.cljc" 324] [odoyle.rules$right_activate_join_node$fn__24665 invoke "rules.cljc" 411] [clojure.lang.PersistentArrayMap kvreduce "PersistentArrayMap.java" 429] [clojure.core$fn__8525 invokeStatic "core.clj" 6908] [clojure.core$fn__8525 invoke "core.clj" 6888] [clojure.core.protocols$fn__8257$G__8252__8266 invoke "protocols.clj" 175] [clojure.core$reduce_kv invokeStatic "core.clj" 6919] [clojure.core$reduce_kv invoke "core.clj" 6910] [odoyle.rules$right_activate_join_node invokeStatic "rules.cljc" 405] [odoyle.rules$right_activate_join_node invoke "rules.cljc" 402] [odoyle.rules$right_activate_alpha_node$fn__24686 invoke "rules.cljc" 456] [clojure.lang.PersistentVector reduce "PersistentVector.java" 343] [clojure.core$reduce invokeStatic "core.clj" 6885] [clojure.core$reduce invoke "core.clj" 6868] [odoyle.rules$right_activate_alpha_node invokeStatic "rules.cljc" 449] [odoyle.rules$right_activate_alpha_node invoke "rules.cljc" 420] [odoyle.rules$upsert_fact$fn__24700 invoke "rules.cljc" 514] [clojure.core.protocols$iter_reduce invokeStatic "protocols.clj" 49] [clojure.core.protocols$fn__8230 invokeStatic "protocols.clj" 75] [clojure.core.protocols$fn__8230 invoke "protocols.clj" 75] [clojure.core.protocols$fn__8178$G__8173__8191 invoke "protocols.clj" 13] [clojure.core$reduce invokeStatic "core.clj" 6886] [clojure.core$reduce invoke "core.clj" 6868] [odoyle.rules$upsert_fact invokeStatic "rules.cljc" 512] [odoyle.rules$upsert_fact invoke "rules.cljc" 485] [odoyle.rules$insert invokeStatic "rules.cljc" 833] [odoyle.rules$insert invoke "rules.cljc" 821] [clojure.lang.AFn applyToHelper "AFn.java" 165] [clojure.lang.AFn applyTo "AFn.java" 144] [clojure.core$apply invokeStatic "core.clj" 669] [clojure.core$apply invoke "core.clj" 662] [pondermatic.rules$eval24942$fn__24944$fn__24948 invoke "rules.cljc" 44] [clojure.lang.ArraySeq reduce "ArraySeq.java" 119] [clojure.core$reduce invokeStatic "core.clj" 6885] [clojure.core$reduce invoke "core.clj" 6868] [pondermatic.rules$eval24942$fn__24944 invoke "rules.cljc" 42] [clojure.lang.MultiFn invoke "MultiFn.java" 234] [pondermatic.rules$process invokeStatic "rules.cljc" 89] [pondermatic.rules$process invoke "rules.cljc" 84] [pondermatic.shell$engine$processor__20971 invoke "shell.cljc" 41] [pondermatic.shell$actor$cr20941_block_4__20946 invoke "shell.cljc" 19] [cloroutine.impl$coroutine$fn__19013 invoke "impl.cljc" 60] [missionary.impl.Ambiguous ready "Ambiguous.java" 351] [missionary.impl.Ambiguous$2 invoke "Ambiguous.java" 440] [missionary.impl.Mailbox post "Mailbox.java" 42] [missionary.impl.Mailbox$Port invoke "Mailbox.java" 21] [pondermatic.shell$_BAR__GT_ invokeStatic "shell.cljc" 48] [pondermatic.shell$_BAR__GT_ invoke "shell.cljc" 45] [pondermatic.engine$db_EQ__GT_rules$update_session__42362 invoke "engine.cljc" 77] [pondermatic.flow$tapper$tapper__20850 invoke "flow.cljc" 23] [missionary.impl.Reduce transfer "Reduce.java" 33] [missionary.impl.Reduce ready "Reduce.java" 55] [missionary.impl.Reduce$1 invoke "Reduce.java" 69] [missionary.impl.Eduction pull "Eduction.java" 115] [missionary.impl.Eduction$2 invoke "Eduction.java" 131] [missionary.impl.Propagator propagate "Propagator.java" 193] [missionary.impl.Propagator tick "Propagator.java" 208] [missionary.impl.Propagator exit "Propagator.java" 232] [missionary.impl.Propagator$1 invoke "Propagator.java" 368] [missionary.impl.Eduction pull "Eduction.java" 115] [missionary.impl.Eduction$2 invoke "Eduction.java" 131] [missionary.impl.Ambiguous$2 invoke "Ambiguous.java" 442] [missionary.impl.Mailbox post "Mailbox.java" 42] [missionary.impl.Mailbox$Port invoke "Mailbox.java" 21] [pondermatic.shell$_BAR__GT_ invokeStatic "shell.cljc" 48] [pondermatic.shell$_BAR__GT_ invoke "shell.cljc" 45] [pondermatic.engine$eval42462$fn__42465 invoke "engine.cljc" 219] [clojure.lang.MultiFn invoke "MultiFn.java" 234] [clojure.core.protocols$fn__8249 invokeStatic "protocols.clj" 168] [clojure.core.protocols$fn__8249 invoke "protocols.clj" 124] [clojure.core.protocols$fn__8204$G__8199__8213 invoke "protocols.clj" 19] [clojure.core.protocols$seq_reduce invokeStatic "protocols.clj" 31] [clojure.core.protocols$fn__8234 invokeStatic "protocols.clj" 75] [clojure.core.protocols$fn__8234 invoke "protocols.clj" 75] [clojure.core.protocols$fn__8178$G__8173__8191 invoke "protocols.clj" 13] [clojure.core$reduce invokeStatic "core.clj" 6886] [clojure.core$reduce invoke "core.clj" 6868] [pondermatic.engine$engine invokeStatic "engine.cljc" 47] [pondermatic.engine$engine invoke "engine.cljc" 39] [pondermatic.shell$engine$processor__20971 invoke "shell.cljc" 41] [pondermatic.shell$actor$cr20941_block_4__20946 invoke "shell.cljc" 19] [cloroutine.impl$coroutine$fn__19013 invoke "impl.cljc" 60] [missionary.impl.Ambiguous ready "Ambiguous.java" 351] [missionary.impl.Ambiguous$2 invoke "Ambiguous.java" 440] [missionary.impl.Mailbox post "Mailbox.java" 42] [missionary.impl.Mailbox$Port invoke "Mailbox.java" 21] [pondermatic.shell$_BAR__GT_ invokeStatic "shell.cljc" 48] [pondermatic.shell$_BAR__GT_ invoke "shell.cljc" 45] [example.predicates$eval42904 invokeStatic "predicates.cljc" 28] [example.predicates$eval42904 invoke "predicates.cljc" 26] [clojure.lang.Compiler eval "Compiler.java" 7194] [clojure.lang.Compiler load "Compiler.java" 7653] [clojure.lang.RT loadResourceScript "RT.java" 381] [clojure.lang.RT loadResourceScript "RT.java" 372] [clojure.lang.RT load "RT.java" 459] [clojure.lang.RT load "RT.java" 424] [clojure.core$load$fn__6908 invoke "core.clj" 6161] [clojure.core$load invokeStatic "core.clj" 6160] [clojure.core$load doInvoke "core.clj" 6144] [clojure.lang.RestFn invoke "RestFn.java" 408] [clojure.core$load_one invokeStatic "core.clj" 5933] [clojure.core$load_one invoke "core.clj" 5928] [clojure.core$load_lib$fn__6850 invoke "core.clj" 5975] [clojure.core$load_lib invokeStatic "core.clj" 5974] [clojure.core$load_lib doInvoke "core.clj" 5953] [clojure.lang.RestFn applyTo "RestFn.java" 142] [clojure.core$apply invokeStatic "core.clj" 669] [clojure.core$load_libs invokeStatic "core.clj" 6016] [clojure.core$load_libs doInvoke "core.clj" 6000] [clojure.lang.RestFn applyTo "RestFn.java" 137] [clojure.core$apply invokeStatic "core.clj" 669] [clojure.core$require invokeStatic "core.clj" 6038] [clojure.core$require doInvoke "core.clj" 6038] [clojure.lang.RestFn invoke "RestFn.java" 408] [clojure.core$map$fn__5935 invoke "core.clj" 2772] [clojure.lang.LazySeq sval "LazySeq.java" 42] [clojure.lang.LazySeq seq "LazySeq.java" 51] [clojure.lang.Cons next "Cons.java" 39] [clojure.lang.RT next "RT.java" 713] [clojure.core$next__5451 invokeStatic "core.clj" 64] [clojure.core$dorun invokeStatic "core.clj" 3143] [clojure.core$dorun invoke "core.clj" 3134] [cognitect.test_runner$test invokeStatic "test_runner.clj" 71] [cognitect.test_runner$test invoke "test_runner.clj" 62] [cognitect.test_runner.api$do_test invokeStatic "api.clj" 14] [cognitect.test_runner.api$do_test invoke "api.clj" 6] [cognitect.test_runner.api$test invokeStatic "api.clj" 28] [cognitect.test_runner.api$test invoke "api.clj" 16] [user$run_test invokeStatic "user.clj" 19] [user$run_test invoke "user.clj" 9] [clojure.lang.Var invoke "Var.java" 384] [clojure.run.exec$exec invokeStatic "exec.clj" 89] [clojure.run.exec$exec invoke "exec.clj" 78] [clojure.run.exec$_main$fn__2254 invoke "exec.clj" 228] [clojure.run.exec$_main invokeStatic "exec.clj" 224] [clojure.run.exec$_main doInvoke "exec.clj" 192] [clojure.lang.RestFn applyTo "RestFn.java" 137] [clojure.lang.Var applyTo "Var.java" 705] [clojure.core$apply invokeStatic "core.clj" 667] [clojure.main$main_opt invokeStatic "main.clj" 514] [clojure.main$main_opt invoke "main.clj" 510] [clojure.main$main invokeStatic "main.clj" 664] [clojure.main$main doInvoke "main.clj" 616] [clojure.lang.RestFn applyTo "RestFn.java" 137] [clojure.lang.Var applyTo "Var.java" 705] [clojure.main main "main.java" 40]], :cause "Could not resolve symbol: ee7b84b6", :data {:type :sci/error, :line 1, :column 21, :file nil, :phase "analysis"}, :runtime :clj}, :runtime :clj, :form (ex-info (ex-message e) {:eval/expr s, :eval/bindings (pr-str (:bindings opts))} e)})