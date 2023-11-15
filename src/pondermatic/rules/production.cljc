(ns pondermatic.rules.production
  (:require [hyperfiddle.rcf :refer [tests tap %]]
            [meander.epsilon :as m]))

(defn throwable? [e]
  (instance? #?(:clj java.lang.Exception :cljs js/Error) e))

(defn parse-pattern [pattern]
  (let [parsed (m/rewrite
                [pattern {::ident :id}]

                [{?ident ?id & ?rest} {::ident ?ident :as ?env}]
                (m/cata [{& ?rest} {:id ?id & ?env}])

                [{& (m/seqable [!as !vs] ...)} {:id (m/some ?id) & ?rest}]
                (m/cata [([?id !as !vs] ...) {& ?rest}])

                (m/and [{& ?rest} {::ident ?ident :as ?env}]
                       (m/let [?id (gensym "?id")]))
                (m/cata [{?ident ?id & ?rest} ?env])


                [([?e ?a ?v] & ?rest) ?env]
                [[?e ?a ?v] & (m/cata [(& ?rest) {::sub-clause true & ?env}])]

                [?expr {::sub-clause true :as ?env}]
                ?expr

                (m/and [?expr ?env]
                       (m/let [?e (ex-info "Failed to parse pattern" {:expr ?expr :env ?env})]))
                ?e)]
    (if (throwable? parsed)
      (throw parsed)
      parsed)))

(defn compile-what [pattern-ast]
  (let [what (m/rewrite
              [pattern-ast {}]

              (m/and [?expr ?env]
                     (m/let [?e (ex-info "Failed to compile expression" {:expr ?expr :env ?env})]))
              ?e)]
    (if (throwable? what)
      (throw what)
      what)))

(defn pattern->what [pattern]
  (-> pattern
      parse-pattern
      compile-what))

(tests
 (pattern->what '?val)
 :throws #?(:clj java.lang.Exception :cljs js/Error)

 (pattern->what '{})
 :throws #?(:clj java.lang.Exception :cljs js/Error)

 (pattern->what ':val)
 :throws #?(:clj java.lang.Exception :cljs js/Error)

 (pattern->what '1)
 :throws #?(:clj java.lang.Exception :cljs js/Error)

 (pattern->what '{:attr ?val})
 := [[_ :attr '?val]]

 (pattern->what '{:attr1 ?val :attr2 :val2})
 := [[?id :attr1 '?val]
     [?id :attr2 :val2]]

 (pattern->what '{:attr1 ?val :db/id ?id})
 := [[?id :attr '?val]]

 (pattern->what '{:attr1 ?val :db/id [:ident ?ident]})
 := [[?id :ident '?ident]
     [?id :attr '?val]]

 (pattern->what '{:db/id ?a
                  :a {:db/id ?id
                      :b :c}})
 := '[[?a :a ?b]
      [?id :b :c]]

 (pattern->what '{:db/id ?a
                  :a {:b :c}})
 := [['?a :a ?b]
     [?b :b :c]]

 (pattern->what '{:tick ?t
                  (skip :a) ?a})
 := [[?id :tick '?t]
     [?id :a '?a {:then false}]]

 (pattern->what '{:db/id ?id
                  (if not= :a) ?a})
 := ['?id :a '?a {:then not=}]

 (pattern->what '{:db/id ?id
                  "(if not= :a)" ?a})
 := ['?id :a '?a {:then not=}])
