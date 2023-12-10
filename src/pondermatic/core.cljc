(ns pondermatic.core
  (:require [pondermatic.db :as db]
            [pondermatic.rules :as rules]
            [pondermatic.engine :as engine]
            [pondermatic.shell :as sh]
            [clojure.walk :as w]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [pondermatic.rules.production :as prod]
            [meander.epsilon :as m]))

(defn ->engine [name & {:keys [:reset-db?] :or {reset-db? false}}]
  (let [db-uri (db/name->mem-uri name)
        conn (db/->conn db-uri reset-db?)
        session (rules/->session)]
    (engine/->engine conn session)))

(defn kw->qkw
  ([data]
   (kw->qkw data "data"))
  ([data ns]
   (w/postwalk (fn [node]
                 #_{:clj-kondo/ignore [:unresolved-symbol]}
                 (if (instance? #?(:clj clojure.lang.IMapEntry :cljs cljs.core.MapEntry)  node)
                   (let [[attr val] node]
                     (if (and (keyword? attr) (nil? (namespace attr)))
                       [(keyword ns (name attr)) val]
                       [attr val]))
                   node))
               data)))

(defn l-vars [data]
  (w/postwalk (fn [node]
                #_{:clj-kondo/ignore [:unresolved-symbol]}
                (if (and (string? node) (re-matches #"^[?].*$" node))
                  (edn/read-string node)
                  node))
              data))

(defn id->ident
  ([data]
   (id->ident data :id))
  ([data id-attr]
   (w/postwalk (fn [node]
                 #_{:clj-kondo/ignore [:unresolved-symbol]}
                 (if (instance? #?(:clj clojure.lang.IMapEntry :cljs cljs.core.MapEntry)  node)
                   (let [[attr val] node]
                     (if (= attr id-attr)
                       [:db/ident (if (string? val)
                                    (edn/read-string (str ":" val))
                                    val)]
                       [attr val]))
                   node))
               data)))

(defn component->entity
  [data]
  (let [inc-fn (fnil inc -1)
        kw-fn (fn [ident attr]
                (let [ns (-> ident
                             str
                             (str/replace "/" ".")
                             (str/replace #"^:" ""))
                      n (-> attr
                            str
                            (str/replace "/" ".")
                            (str/replace #"^:" ""))]
                  (keyword ns n)))]
    (m/rewrite
     [data {:ident :item}]

     (m/and [{::attr (m/some ?attr) ::value (m/some ?value)} {:ident ?id & ?env}]
            (m/let [?ident (kw-fn ?id ?attr)]))
     [?attr (m/cata [?value {:ident ?ident & ?env}])]

     (m/and [[?item] {:ident ?id}]
            (m/let [?m-idx (inc-fn nil)
                    ?ident (kw-fn ?id ?m-idx)]))
     [(m/cata [?item {:ident ?ident :idx ?m-idx}])]

     (m/and [[?item & ?rest] {:ident ?id :idx ?idx & ?env}]
            (m/let [?m-idx (inc-fn ?idx)
                    ?ident (kw-fn ?id ?m-idx)]))
     [(m/cata [?item {:ident ?ident :idx ?m-idx & ?env}])
      & (m/cata [[& ?rest] {:ident ?ident :idx ?m-idx & ?env}])]

     [{:db/ident (m/some ?ident) & ?rest} {:ident _ & ?env}]
     {:db/ident ?ident
      & (m/cata [{& ?rest} {:type :entity :ident ?ident & ?env}])}

     [{& (m/seqable [!k !v] ...)} {:type :entity & ?env}]
     {& [(m/cata [{::attr !k ::value !v} ?env]) ...]}

     [{& (m/some ?rest)} {:ident ?ident & ?env}]
     (m/cata [{:db/ident ?ident & ?rest} {:type :entity :ident ?ident & ?env}])

     [{} ?env]
     {}

     [?expr ?env]
     ?expr)))

(def |> sh/|>)

(def |< sh/|<)

(def |>< sh/|><)

(def type-name engine/type-name)

(def rule-type engine/rule-type)

(defn parse-patterns [ruleset]
  (mapv (fn [rule]
          (-> rule
              (update :rule/when #(prod/parse-pattern % {}))
              (update :rule/then #(prod/parse-gen-pattern %))))
        ruleset))

(defn ruleset [rules]
  (-> (map #(assoc % type-name rule-type) rules)
      id->ident
      kw->qkw
      l-vars
      parse-patterns
      component->entity))

(defn dataset [data & {:keys [id-attr ns] :or {id-attr :id ns "data"}}]
  (-> data
      (id->ident id-attr)
      (kw->qkw ns)
      component->entity))

(def conn> engine/conn>)

(def rule-atom engine/rule-atom)

(def rules<
  (sh/|<= (map ::rules)))

(def q<> engine/q<>)
