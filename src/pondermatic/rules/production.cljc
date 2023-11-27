(ns pondermatic.rules.production
  (:require [hyperfiddle.rcf :refer [tests]]
            [meander.epsilon :as m]
            [clojure.edn :as edn]
            [sci.core :as sci]))

(defn throwable? [e]
  (instance? #?(:clj java.lang.Exception :cljs js/Error) e))

(defn parse-pattern [pattern opts]
  (let [defaults {:identity :id}
        env (merge defaults opts)
        parsed (m/rewrite
                [pattern env]

                (m/and [[?contains] ?env]
                       (m/let [?id (gensym "?id-")]))
                {:tag :contains
                 :id ?id
                 :contains (m/cata [?contains ?env])}

                [{?identier [?ident ?id] & ?rest}
                 {:identity ?identier :as ?env}]
                (m/cata [{?ident ?id & ?rest} ?env])

                [{?identier (m/some ?id) & ?rest}
                 {:identity ?identier :as ?env}]
                {:tag :join
                 :id ?id
                 :select (m/cata [{& ?rest}
                                  {:part :sub-clause :type :select & ?env}])}

                [{& (m/seqable [!attr !val] ...)}
                 {:part :sub-clause :type :select & ?env}]
                [{:tag :project
                  :attr (m/cata [!attr {:part :sub-clause :type :attr & ?env}])
                  :val (m/cata [!val {:part :sub-clause :type :val & ?env}])} ...]

                (m/and [(m/pred map? ?m) {:identity ?identier :as ?env}]
                       (m/let [?id (gensym "?id-")]))
                (m/cata [{?identier ?id & ?m} ?env])

                [(m/symbol _ (m/re #"^[?].+") :as ?symbol) {:part :sub-clause}]
                {:tag :logic-variable
                 :symbol ?symbol}

                (m/and [(m/pred string? ?attr-str) {:part :sub-clause :type :attr :as ?env}]
                       (m/let [?attr (edn/read-string ?attr-str)]))
                (m/cata [?attr ?env])

                [(?mod ?attr) {:part :sub-clause :type :attr :as ?env}]
                {:tag :modifier
                 :modifier ?mod
                 :attr (m/cata [?attr ?env])}

                [?attr {:part :sub-clause :type :attr}]
                {:tag :attribute
                 :attribute ?attr}

                [?value {:part :sub-clause}]
                {:tag :value
                 :value ?value}

                (m/and [?expr ?env]
                       (m/let [?e (throw (ex-info "Failed to parse expression" {:expr ?expr :env ?env}))]))
                ?e)]
    (if (throwable? parsed)
      (throw parsed)
      parsed)))

(defn compile-what [pattern-ast]
  (let [what (m/rewrite
              [pattern-ast {}]

              [[?e ?a ?v nil] nil]
              [[?e ?a ?v]]

              [[?e ?a ?v (m/pred map? ?mod)] nil]
              [[?e ?a ?v ?mod]]

              [{:tag :value :value ?value} _]
              ?value

              [{:tag :attribute :attribute ?attr} _]
              ?attr

              [{:tag :logic-variable :symbol ?symbol} _]
              ?symbol

              [{:tag :modifier :modifier :skip} _]
              {:then false}

              (m/and [{:tag :modifier
                       :modifier (m/pred symbol? ?mod)} _]
                     (m/let [?fn (sci/eval-string (str ?mod))]))
              {:then ?fn}

              [{:tag :contains
                :id ?id
                :contains {:id ?item-id
                           :as ?contains}} ?env]
              [[?id :a/contains ?item-id]
               & (m/cata [?contains {:id ?id & ?env}])]

              [{:tag :join
                :id ?id
                :select []} _]
              []

              [{:tag :join
                :id ?id
                :select [?project]} ?env]
              [& (m/cata [?project {:id ?id & ?env}])]

              [{:tag :join
                :id ?id
                :select [?project & ?rest]} ?env]
              [& (m/cata [?project {:id ?id & ?env}])
               & (m/cata [{:tag :join
                           :id ?id
                           :select [& ?rest]} ?env])]

              [{:tag :project
                :attr {:modifier (m/some)
                       :attr ?attr
                       :as ?mod}
                & ?project} ?env]
              (m/cata [{:tag :project
                        :modifier (m/cata [?mod ?env])
                        :attr ?attr
                        & ?project} ?env])

              [{:tag :project
                :attr ?attr
                :modifier ?mod
                :val {:id (m/some ?join-id) :as ?val}}
               {:id (m/some ?id) :as ?env}]
              [& (m/cata [[?id (m/cata [?attr ?env]) ?join-id ?mod] nil])
               & (m/cata [?val ?env])]

              [{:tag :project
                :attr ?attr
                :modifier ?mod
                :val ?val}
               {:id (m/some ?id) :as ?env}]
              (m/cata [[?id (m/cata [?attr ?env]) (m/cata [?val ?env]) ?mod] nil])

              (m/and [?expr ?env]
                     (m/let [?e (throw (ex-info "Failed to compile expression" {:expr ?expr :env ?env}))]))
              ?e)]
    (if (throwable? what)
      (throw what)
      what)))

(defn pattern->what
  ([pattern]
   (pattern->what pattern {}))
  ([pattern opts]
   (-> pattern
       (parse-pattern opts)
       compile-what)))

(tests
 (defn ! [x] (prn x) x)

 (parse-pattern {} {}) := {:tag :join
                           :id ?id
                           :select []}

 (parse-pattern [{}] {}) := {:tag :contains
                             :id ?a
                             :contains {:tag :join
                                        :id ?id
                                        :select []}}

 (parse-pattern '{:id ?id} {}) := {:tag :join
                                   :id '?id
                                   :select []}

 (parse-pattern '{:id ?id :attr :val} {}) := {:tag :join
                                              :id '?id
                                              :select [{:tag :project
                                                        :attr {:tag :attribute
                                                               :attribute :attr}
                                                        :val {:tag :value
                                                              :value :val}}]}

 (parse-pattern '{:id ?id :attr ?val} {}) := {:tag :join
                                              :id '?id
                                              :select [{:tag :project
                                                        :attr {:tag :attribute
                                                               :attribute :attr}
                                                        :val {:tag :logic-variable
                                                              :symbol '?val}}]}

 (parse-pattern '{:attr :val} {}) := {:tag :join
                                      :id ?id
                                      :select [{:tag :project
                                                :attr {:tag :attribute
                                                       :attribute :attr}
                                                :val {:tag :value
                                                      :value :val}}]}
 (parse-pattern '{:id :id1
                  :attr {:id :id2
                         :attr2 :val}} {}) := {:tag :join
                                               :id :id1
                                               :select [{:tag :project
                                                         :attr {:tag :attribute
                                                                :attribute :attr}
                                                         :val {:tag :join
                                                               :id :id2
                                                               :select [{:tag :project
                                                                         :attr {:tag :attribute
                                                                                :attribute :attr2}
                                                                         :val {:tag :value
                                                                               :value :val}}]}}]}

 (pattern->what '?val)
 :throws #?(:clj java.lang.Exception :cljs js/Error)

 (pattern->what ':val)
 :throws #?(:clj java.lang.Exception :cljs js/Error)

 (pattern->what '1)
 :throws #?(:clj java.lang.Exception :cljs js/Error)

 (pattern->what '{})
 := []

 (pattern->what '{:id ?id})
 := []

 (pattern->what '{:attr ?val})
 := [[_ :attr '?val]]

 (pattern->what '[{:attr ?val :id :id}])
 := [[?id :a/contains :id]
     [:id :attr '?val]]

 (pattern->what '{:attr [{:attr2 ?val}]})
 := [[?a :attr ?b]
     [?b :a/contains ?c]
     [?c :attr2 '?val]]

 (pattern->what '{:attr1 ?val :attr2 :val2})
 := [[?id :attr1 '?val]
     [?id :attr2 :val2]]

 (pattern->what '{:attr1 ?val :db/id ?id} {:identity :db/id})
 := '[[?id :attr1 ?val]]

 (pattern->what '{:attr ?val :id [:db/id ?ident]})
 := [[?id :attr '?val]
     [?id :db/id '?ident]]

 (pattern->what '{:id ?a
                  :a {:id ?id
                      :b :c}})
 := '[[?a :a ?id]
      [?id :b :c]]

 (pattern->what '{:id ?a
                  :a {:b :c}})
 := [['?a :a ?b]
     [?b :b :c]]

 (pattern->what '{:tick ?t
                  (:skip :a) ?a})
 := [[?id :tick '?t]
     [?id :a '?a {:then false}]]

 (pattern->what '{:id ?id
                  (not= :a) ?a})
 := [['?id :a '?a {:then not=}]]

 (pattern->what '{:id ?id
                  "(not= :a)" ?a})
 := [['?id :a '?a {:then not=}]]

 nil)
