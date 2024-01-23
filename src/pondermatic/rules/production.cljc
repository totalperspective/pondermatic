(ns pondermatic.rules.production
  (:require [hyperfiddle.rcf :refer [tests]]
            [meander.epsilon :as m]
            [clojure.edn :as edn]
            [sci.core :as sci]
            [hasch.core :as h]
            [hasch.benc :as hb]
            [portal.console :as log]
            [camel-snake-kebab.core :as csk]
            [clojure.string :as str]
            [inflections.core :as i]
            [clojure.walk :as w]
            [tick.core :as t]
            [incognito.base :as ib]
            #?(:cljs
               [java.time :refer [LocalDate LocalDateTime]]))
  #?(:clj
     (:import [java.time LocalDate LocalDateTime])))

(def write-handlers
  {`LocalDate (fn [d] (str d))
   `LocalDateTime (fn [dt] (str dt))
   (symbol "#object[LocalDate]") (fn [d] (str d))
   (symbol "#object[LocalDateTime]") (fn [d] (str d))})

(def read-handlers
  {`LocalDate (fn [d] (t/date d))
   `LocalDateTime (fn [dt] (t/date-time dt))})

(extend-protocol
 hb/PHashCoercion
  LocalDate
  (-coerce [this md-create-fn write-handlers]
    (hb/-coerce
     (ib/incognito-writer write-handlers this)
     md-create-fn
     write-handlers)))

(defn uuid-hash [x]
  (h/uuid x :write-handlers write-handlers))

(def nss
  (let [add_ #(str/replace % " " "_")
        normalize (comp add_ str/lower-case)
        t (sci/create-ns 'tick.core)
        sci-t-ns (sci/copy-ns tick.core t {:exclude []})]
    {'hash {'uuid uuid-hash
            'squuid h/squuid
            'b64 h/b64-hash}
     'inflection {'plural i/plural
                  'singular i/singular
                  'ordinalize i/ordinalize}
     'case {'normalize normalize
            'upper str/upper-case
            'lower str/lower-case
            'camel (comp csk/->camelCase normalize)
            'kebab (comp csk/->kebab-case normalize)
            'snake (comp csk/->snake_case normalize)}
     't sci-t-ns}))

(defn throwable? [e]
  (instance? #?(:clj java.lang.Exception :cljs js/Error) e))

(defn parse-pattern [pattern opts]
  (when pattern
    (log/trace {:parse/pattern pattern
                :parse/opts opts})
    (let [defaults {:identity :id :part :clause}
          env (merge defaults opts)
          parsed (m/rewrite
                  [pattern env]

                  [(m/pred set? ?context (m/seqable !clauses ...)) {:part :sub-clause :as ?env}]
                  {::tag :application
                   ::fn 'hash-set
                   ::args [(m/cata [!clauses {:context ?context & ?env}]) ...]}

                  [(m/pred set? ?context (m/seqable !clauses ...)) ?env]
                  {::tag :conjunction
                   ::clauses [(m/cata [!clauses {:context ?context & ?env}]) ...]}

                  (m/and [[?contains :as ?context] ?env]
                         (m/let [?item-id (gensym "?production-id-")
                                 ?node-id (gensym "?production-id-")]))
                  {::tag :contains
                   ::id ?item-id
                   ::node-id ?node-id
                   ::contains (m/cata [?contains {:context ?context & ?env}])}

                  [{(m/symbol "&") ?id & ?rest :as ?context}
                   {:identity ?identier :as ?env}]
                  (m/cata [{?identier ?id & ?rest} {:context ?context & ?env}])

                  [{?identier [?ident ?id] & ?rest :as ?context}
                   {:identity ?identier :as ?env}]
                  (m/cata [{?ident ?id & ?rest} {:context ?context & ?env}])

                  [{?identier (m/some ?id) & ?rest :as ?context}
                   {:identity ?identier :as ?env}]
                  {::tag :join
                   ::id ?id
                   ::select (m/cata [{& ?rest}
                                     {:part :sub-clause
                                      :type :select
                                      :context ?context
                                      & ?env}])}

                  [{& (m/seqable [!attr !val] ...) :as ?context}
                   {:part :sub-clause :type :select & ?env}]
                  [{::tag :project
                    ::attr (m/cata [!attr {:part :sub-clause :type :attr :context ?context & ?env}])
                    ::val (m/cata [!val {:part :sub-clause :type :val :context ?context & ?env}])} ...]

                  (m/and ?context
                         [(m/pred map? ?m) {:identity ?identier :as ?env}]
                         (m/let [?id (gensym "?production-id-")]))
                  (m/cata [{?identier ?id & ?m} {:context ?context & ?env}])

                  [(m/symbol _ (m/re #"^[?].+") :as ?symbol) {:part :sub-clause}]
                  {::tag :logic-variable
                   ::symbol ?symbol}

                  (m/and ?context
                         [(m/pred string? ?attr-str) {:part :sub-clause :type :attr :as ?env}]
                         (m/let [?attr (edn/read-string ?attr-str)]))
                  (m/cata [?attr {:context ?context & ?env}])

                  [(?mod ?attr :as ?context) {:part :sub-clause :type :attr :as ?env}]
                  {::tag :modifier
                   ::modifier ?mod
                   ::attr (m/cata [?attr {:context ?context & ?env}])}

                  [?attr {:part :sub-clause :type :attr}]
                  {::tag :attribute
                   ::attribute ?attr}

                  [nil {:part :sub-clause}]
                  {::tag :value
                   ::value :a/nil}

                  [?value {:part :sub-clause}]
                  {::tag :value
                   ::value ?value}


                  [(!pred ..1 !args ... :as ?context) {:part :clause & ?env}]
                  {::tag :predicate
                   ::predicate !pred
                   ::args [(m/cata [!args {:part :sub-clause :context ?context & ?env}]) ...]}

                  (m/and [?expr ?env]
                         (m/let [?e (throw (ex-info "Failed to parse pattern" {:expr ?expr :env ?env}))]))
                  ?e)]
      (if (throwable? parsed)
        (throw parsed)
        parsed))))

(defn compile-what [pattern-ast]
  (let [what (m/rewrite
              [pattern-ast {}]

              [[?e ?a ?v nil] nil]
              [[?e ?a ?v]]

              [[?e ?a ?v (m/pred map? ?mod)] nil]
              [[?e ?a ?v ?mod]]

              [{::tag :conjunction
                ::clauses []} _]
              []

              [{::tag :conjunction
                ::clauses [?first]} ?env]
              [& (m/cata [?first ?env])]

              [{::tag :conjunction
                ::clauses [?first & ?rest]} ?env]
              [& (m/cata [?first ?env])
               & (m/cata [{::tag :conjunction
                           ::clauses [& ?rest]} ?env])]

              [{::tag :value ::value ?value} _]
              ?value

              [{::tag :attribute ::attribute ?attr} _]
              ?attr

              [{::tag :logic-variable ::symbol ?symbol} _]
              ?symbol

              [{::tag :modifier ::modifier :skip} _]
              {:then false}

              (m/and [{::tag :modifier
                       ::modifier (m/pred symbol? ?mod)} _]
                     (m/let [?fn (sci/eval-string (str ?mod))]))
              {:then ?fn}

              [{::tag :contains
                ::id ?id
                ::node-id ?node-id
                ::contains {::id ?item-id
                            :as ?contains}}
               {:id (m/some ?parent-id) :attr ?attr & ?env}]
              [[?node-id :p/contained-by ?parent-id]
               [?node-id :p/attr (m/cata [?attr ?env])]
               [?node-id :a/first ?item-id]
               & (m/cata [?contains {:id ?item-id & ?env}])]

              [{::tag :contains
                ::id ?id
                ::node-id ?node-id
                ::attr nil
                ::contains {::id ?item-id
                            :as ?contains}} ?env]
              [[?node-id :p/contained-by ?id]
               [?node-id :a/first  ?item-id]
               & (m/cata [?contains {:id ?id & ?env}])]

              [{::tag :join
                ::id ?id
                ::select []} _]
              []

              [{::tag :join
                ::id ?id
                ::select [?project]} ?env]
              [& (m/cata [?project {:id ?id & ?env}])]

              [{::tag :join
                ::id ?id
                ::select [?project & ?rest]} ?env]
              [& (m/cata [?project {:id ?id & ?env}])
               & (m/cata [{::tag :join
                           ::id ?id
                           ::select [& ?rest]} ?env])]

              [{::tag :project
                ::attr {::modifier (m/some)
                        ::attr ?attr
                        :as ?mod}
                & ?project} ?env]
              (m/cata [{::tag :project
                        ::modifier (m/cata [?mod ?env])
                        ::attr ?attr
                        & ?project} ?env])

              [{::tag :project
                ::attr ?attr
                ::modifier ?mod
                ::val {::tag :contains :as ?val}}
               {:id (m/some ?id) :as ?env}]
              [& (m/cata [?val {:attr ?attr :id ?id & ?env}])]

              [{::tag :project
                ::attr ?attr
                ::modifier ?mod
                ::val {::id (m/some ?join-id)
                       :as ?val}}
               {:id (m/some ?id) :as ?env}]
              [& (m/cata [[?id (m/cata [?attr ?env]) ?join-id ?mod] nil])
               & (m/cata [?val ?env])]

              [{::tag :project
                ::attr ?attr
                ::modifier ?mod
                ::val ?val}
               {:id (m/some ?id) :as ?env}]
              (m/cata [[?id (m/cata [?attr ?env]) (m/cata [?val ?env]) ?mod] nil])


              [{::tag :application} _]
              nil

              [{::tag :predicate} _]
              nil

              (m/and [?expr ?env]
                     (m/let [?e (throw (ex-info "Failed to compile expression" {:expr ?expr :env ?env}))]))
              ?e)]
    (if (throwable? what)
      (throw what)
      what)))

(defn compile-when [pattern-ast env]
  (let [what (m/rewrite
              [pattern-ast env]

              [{::tag :conjunction
                ::clauses []} _]
              []

              [{::tag :conjunction
                ::clauses [?first]} ?env]
              [& (m/cata [?first ?env])]

              [{::tag :conjunction
                ::clauses [?first & ?rest]} ?env]
              [& (m/cata [?first ?env])
               & (m/cata [{::tag :conjunction
                           ::clauses [& ?rest]} ?env])]

              [{::tag :value ::value ?value} _]
              ?value

              [{::tag :attribute ::attribute ?attr} _]
              nil

              [{::tag :logic-variable ::symbol ?symbol} _]
              ?symbol

              [{::tag :contains} _]
              nil

              [{::tag :join} _]
              nil

              [{::tag :project} ?env]
              nil

              [{::tag :predicate
                ::predicate (m/some ?pred)
                ::args [!args ...]}
               {scope {?pred (m/some ?fn)} :as ?env}]
              [(?fn & [(m/cata [!args ?env]) ...])]

              [{::tag :predicate
                ::predicate (m/pred symbol? ?symbol)
                ::args [!args ...]} ?env]
              [(?symbol & [(m/cata [!args ?env]) ...])]

              [{::tag :application
                ::fn (m/pred symbol? ?fn)
                ::args [!args ...]} ?env]
              (?fn & [(m/cata [!args ?env]) ...])

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

(defn pattern->when
  ([pattern]
   (pattern->when pattern {}))
  ([pattern opts]
   (-> pattern
       (parse-pattern opts)
       (compile-when opts))))

(defn eval-expr [expr env]
  (log/trace {:eval/expr expr
              :eval/env env})
  (let [vars (reduce-kv (fn [m k v]
                          (assoc m k (sci/new-var k v)))
                        {} env)]
    (sci/eval-string (str expr)
                     {:namespaces (assoc nss 'user vars)})))

(defn parse-gen-pattern [pattern]
  (m/rewrite
   [pattern {}]

   (m/and [(m/pred string? (m/re #"^\[\$ .*\]$") ?str) ?env]
          (m/let [?expr (edn/read-string ?str)]))
   (m/cata [?expr ?env])

   [[$ ?expr ?merge] ?env]
   {::tag :aggregate
    ::merge ?merge
    ::expr (m/cata [[$ ?expr] ?env])}

   (m/and [[$ ?expr] ?env]
          (m/let [?expr-str (str ?expr)]))
   {::tag :expr
    ::expr ?expr-str}

   [(m/symbol _ (m/re #"^[?].+") :as ?symbol) ?env]
   {::tag :logic-variable
    ::symbol ?symbol}

   [{:key (m/some ?k) :value ?v} ?env]
   {::tag :map-entry
    ::key ?k
    ::value (m/cata [?v ?env])}

   [{(m/symbol "&") ?e & ?rest} ?env]
   {::entity ?e
    & (m/cata [{& ?rest} ?env])}

   [{& (m/seqable [!k !v] ...)} ?env]
   {::tag :map
    ::entries [(m/cata [{:key !k :value !v} ?env]) ...]}

   [[!items ...] ?env]
   {::tag :sequence
    ::type :vector
    ::items [(m/cata [!items ?env]) ...]}

   [(!items ...) ?env]
   {::tag :sequence
    ::type :list
    ::items [(m/cata [!items ?env]) ...]}

   [(m/and (m/pred set?) (m/seqable !items ...)) ?env]
   {::tag :sequence
    ::type :set
    ::items [(m/cata [!items ?env]) ...]}

   [?expr ?env]
   {::tag :value
    ::value ?expr}))

(defn unify-gen-pattern [pattern env]
  (m/rewrite
   [pattern env]

   (m/and [(merge ?a ?b) ?env]
          (m/let [?e (merge ?a ?b)]))
   ?e

   (m/and [(hash-set & ?rest) ?env]
          (m/let [?e (apply hash-set ?rest)]))
   ?e

   (m/and [{::tag :expr ::expr ?expr} ?env]
          (m/let [?result (eval-expr ?expr ?env)]))
   ?result

   [{::tag :logic-variable ::symbol ?symbol} {?symbol (m/some ?val) & ?env}]
   ?val

   (m/and [{::tag :logic-variable ::symbol ?symbol} ?env]
          (m/let [?e (throw (ex-info "Failed to unify symbol" {:symbol ?symbol :env ?env}))]))
   ?e

   (m/and [{::tag :map-entry ::key (m/pred string? ?k-str) ::value ?v} ?env]
          (m/let [?k (edn/read-string ?k-str)]))
   (m/cata [{::tag :map-entry ::key ?k ::value ?v} ?env])

   (m/and [{::tag :map-entry ::key (m/pred keyword? (m/re #"^::") ?k-kw) ::value ?v} ?env]
          (m/let [[_ ?k-str] (re-matches #"^:(.*)$" (str ?k-kw))
                  ?k (edn/read-string ?k-str)]))
   (m/cata [{::tag :map-entry ::key ?k ::value ?v} ?env])

   [{::tag :map-entry ::key ?k ::value ?v} ?env]
   [?k (m/cata [?v ?env])]

   [{::tag :value ::value ?expr} ?env]
   ?expr

   [{::entity (m/some ?e) & ?rest} {?e ?id entities {?id ?entity} :as ?env}]
   (m/cata [(merge ?entity {& (m/cata [{& ?rest} ?env])}) ?env])

   [{::tag :map ::entries [!entries ...]} ?env]
   {& [(m/cata [!entries ?env]) ...]}

   [{::tag :sequence ::type :vector ::items [!items ...]} ?env]
   [(m/cata [!items ?env]) ...]

   [{::tag :sequence ::type :list ::items [!items ...]} ?env]
   ((m/cata [!items ?env]) ...)

   [{::tag :sequence ::type :set ::items [!items ...]} ?env]
   (m/cata [(hash-set (m/cata [!items ?env]) ...) ?env])

   (m/and [{::tag :aggregate ::merge ?merge ::expr ?expr :as ?agg} ?env]
          (m/let [?id (uuid-hash ?agg)
                  ?val (unify-gen-pattern ?expr ?env)
                  ?agg-fn (fn agg-fn
                            ([] ?id)
                            ([other] (if (nil? other)
                                       ?val
                                       (agg-fn other ?val)))
                            ([x y] (sci/eval-string (str (list ?merge x y)))))]))
   ?agg-fn

   (m/and [?expr ?env]
          (m/let [?e (throw (ex-info "Failed to unify expr" {:expr ?expr :env ?env}))]))
   ?e))


(defn unify*
  ([pattern bindings]
   (unify* pattern bindings {:id :unify-pattern :entities {}}))
  ([pattern bindings env]
   (let [{:keys [id entities]} env
         ?id id]
     (->> bindings
          (map (fn [b]
                 (unify-gen-pattern pattern (update b 'entities #(merge (or % {})
                                                                        entities)))))
          (reduce  (fn [m p]
                     (let [p' (w/postwalk (fn [node]
                                            (if (fn? node)
                                              (node)
                                              node))
                                          p)
                           id (uuid-hash p')]
                       (merge-with
                        (fn merge-fn [x y]
                          (cond
                            (map? x) (merge-with merge-fn x y)

                            (and (fn? x) (fn? y)) (comp y x)

                            (fn? y) y

                            (= x y) x

                            (nil? x) y

                            :else (throw (ex-info "Couldn't perfoprm merge"
                                                  {:rule ?id :args [x y]}))))
                        m

                        {id p})))
                   {})
          vals
          (map (fn [p]
                 (w/postwalk (fn [node]
                               (if (fn? node)
                                 (node nil)
                                 node))
                             p)))))))

(defn unify-pattern [pattern bindings]
  (let [[env return] (if (sequential? bindings)
                       [bindings vec]
                       [[bindings] first])]
    (-> pattern
        parse-gen-pattern
        (unify* env)
        return)))

(tests
 (defn ! [x] (prn x) x)

 (parse-pattern {} {})
 := {::tag :join
     ::id ?id
     ::select []}

 (parse-pattern [{}] {})
 := {::tag :contains
     ::id ?a
     ::node-id ?b
     ::contains {::tag :join
                 ::id ?id
                 ::select []}}

 (parse-pattern #{{}} {})
 := {::tag :conjunction
     ::clauses [{::tag :join
                 ::id ?id
                 ::select []}]}

 (parse-pattern '{:id ?id} {})
 := {::tag :join
     ::id '?id
     ::select []}

 (parse-pattern '{:id ?id :attr :val} {})
 := {::tag :join
     ::id '?id
     ::select [{::tag :project
                ::attr {::tag :attribute
                        ::attribute :attr}
                ::val {::tag :value
                       ::value :val}}]}

 (parse-pattern '{:attr1 :val1 :attr2 :val2} {})
 := {::tag :join
     ::id ?id
     ::select [{::tag :project
                ::attr {::tag :attribute
                        ::attribute :attr1}
                ::val {::tag :value
                       ::value :val1}}
               {::tag :project
                ::attr {::tag :attribute
                        ::attribute :attr2}
                ::val {::tag :value
                       ::value :val2}}]}

 (parse-pattern '{:id ?id :attr ?val} {})
 := {::tag :join
     ::id '?id
     ::select [{::tag :project
                ::attr {::tag :attribute
                        ::attribute :attr}
                ::val {::tag :logic-variable
                       ::symbol '?val}}]}

 (parse-pattern '{:attr :val} {})
 := {::tag :join
     ::id ?id
     ::select [{::tag :project
                ::attr {::tag :attribute
                        ::attribute :attr}
                ::val {::tag :value
                       ::value :val}}]}

 (parse-pattern '(pred ?lhs 1) {})
 := '{::tag :predicate
      ::predicate pred
      ::args [{::tag :logic-variable
               ::symbol ?lhs}
              {::tag :value
               ::value 1}]}

 (parse-pattern '{:id :id1
                  :attr {:id :id2
                         :attr2 :val}} {})
 := {::tag :join
     ::id :id1
     ::select [{::tag :project
                ::attr {::tag :attribute
                        ::attribute :attr}
                ::val {::tag :join
                       ::id :id2
                       ::select [{::tag :project
                                  ::attr {::tag :attribute
                                          ::attribute :attr2}
                                  ::val {::tag :value
                                         ::value :val}}]}}]}

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

 (pattern->what '{:attr nil})
 := [[_ :attr :a/nil]]

 (pattern->what '{:attr :val & ?id})
 := [['?id :attr :val]]

 (pattern->what '{":db/ident" :ident :attr nil})
 := [[?id :db/ident :ident]
     [?id :attr :a/nil]]

 (pattern->what '{:attr ?val})
 := [[_ :attr '?val]]

 (pattern->what '#{})
 := []

 (pattern->what '#{{:attr ?val}})
 := [[?a :attr '?val]]

 (pattern->what '#{{:attr ?val}
                   {:attr2 ?val}})
 := [[?a :attr2 '?val]
     [?b :attr '?val]]

 (pattern->what '[{:attr ?val :id :id}])
 := [[?a :p/contained-by ?id]
     [?id :a/first :id]
     [:id :attr '?val]]

 (pattern->what '{:attr [{:attr2 ?val}]})
 := [[?b :p/contained-by ?a]
     [?b :p/attr :attr]
     [?b :a/first ?c]
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

 (pattern->what '(pred ?lhs ?rhs))
 := nil

 (pattern->what '#{(pred ?lhs ?rhs)})
 := []

 (pattern->when '{})
 := nil

 (pattern->when '[{}])
 := nil

 (pattern->when '#{(and ?lhs ?rhs)})
 := '[(and ?lhs ?rhs)]

 (pattern->when '#{(contains? #{"item1" "item2"} ?rhs)})
 := '[(contains? (hash-set "item1" "item2") ?rhs)]

 (pattern->when '#{(">" ?lhs 1)} '{scope {">" `>}})
 := '[(`> ?lhs 1)]

 (unify-pattern '?x '{?x 1}) := 1

 (unify-pattern '?y '{?x 1})
 :throws #?(:clj java.lang.Exception :cljs js/Error)

 (unify-pattern '{:x ?x} '{?x 1}) := {:x 1}

 (unify-pattern '[?x] '{?x 1 ?y 2}) := [1]

 (unify-pattern '[?x ?y] '{?x 1 ?y 2}) := [1 2]

 (unify-pattern  '{:x ?x :y ?y} '{?x 1 ?y 2}) := {:x 1 :y 2}

 (unify-pattern '{":x" ?x} '{?x 1}) := {:x 1}

 (unify-pattern '{:y {:x ?x}} '{?x 1}) := {:y {:x 1}}

 (unify-pattern (str '[$ (inc ?x)]) '{?x 1}) := 2

 (unify-pattern '{& ?e} '{?e 1 entities {1 {:foo :bar}}})
 := {:foo :bar}

 (unify-pattern '{:attr :val & ?e} '{?e 1 entities {1 {:foo :bar}}})
 := {:attr :val :foo :bar}

 (unify-pattern '[$ "(case/kebab ?name)"] '{?name "foo"}) := "foo"
 nil)
