(ns pondermatic.rules.production
  (:require [hyperfiddle.rcf :refer [tests]]
            [meander.epsilon :as m]))

(defn throwable? [e]
  (instance? #?(:clj java.lang.Exception :cljs js/Error) e))

(defn parse-pattern [pattern opts]
  (let [defaults {::identity :id}
        env (merge defaults opts)
        parsed (m/rewrite
                [pattern env]

                [(m/symbol _ (m/re #"^?.+") :as ?symbol) _]
                {:tag :logic-variable
                 :symbol ?symbol}

                [?value _]
                {:tag :value
                 :value ?value})]
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
      (parse-pattern {})
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
