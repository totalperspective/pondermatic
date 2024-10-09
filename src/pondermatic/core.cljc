(ns pondermatic.core
  (:refer-clojure :exclude [import])
  (:require [pondermatic.db :as db]
            [pondermatic.rules :as rules]
            [pondermatic.engine :as engine]
            [pondermatic.shell :as sh]
            [pondermatic.flow :as flow]
            [clojure.walk :as w]
            [clojure.string :as str]
            [pondermatic.reader :as pr]
            [pondermatic.rules.production :as prod]
            [meander.epsilon :as m]
            [edn-query-language.core :as eql]))

(defn ->engine [name & {:keys [:reset-db?] :or {reset-db? false}}]
  (let [db-uri (db/name->mem-uri name)
        conn (db/->conn db-uri reset-db?)
        session (rules/->session)]
    (assoc (engine/->engine conn session)
           ::q! (partial db/q! conn))))

(def clone> engine/clone>)

(defn kw->qkw
  ([data]
   (kw->qkw data "data"))
  ([data ns]
   (w/postwalk (fn [node]
                 #_{:clj-kondo/ignore [:unresolved-symbol]}
                 (if (map-entry? node)
                   (let [[attr val] node]
                     (if (and (keyword? attr) (nil? (namespace attr)))
                       [(keyword ns (name attr)) val]
                       [attr val]))
                   node))
               data)))

(defn parse-strings [data]
  (w/postwalk (fn [node]
                #_{:clj-kondo/ignore [:unresolved-symbol]}
                (cond
                  (and (string? node) (re-matches #"^[+].*$" node))
                  (pr/-read-string (apply str (rest node)))

                  (and (string? node) (re-matches #"^[?:].*$" node))
                  (pr/-read-string node)

                  (map-entry? node)
                  (let [[key val] node]
                    (if (list? key)
                      (let [[mod key] key]
                        [(list mod (keyword (str key))) val])
                      node))

                  :else node))
              data))

(defn id->ident
  ([data]
   (id->ident data :id))
  ([data id-attr]
   (w/postwalk (fn [node]
                 #_{:clj-kondo/ignore [:unresolved-symbol]}
                 (cond
                   (map-entry? node)
                   (let [[attr val] node]
                     (if (= attr id-attr)
                       [:db/ident (cond
                                    (and (string? val)
                                         (#{\# \:} (first val)))
                                    (pr/-read-string val)

                                    (string? val)
                                    (pr/-read-string (str ":" val))

                                    (and (symbol? val)
                                         (not (#{\+ \?} (first (str val)))))
                                    (pr/-read-string (str ":" val))

                                    :else
                                    val)]
                       [attr val]))

                   (and (vector? node)
                        (= (count node) 2)
                        (= (str (first node)) "id"))
                   (pr/-read-string (str ":" (second node)))

                   :else
                   node))
               data)))

(defn component->entity
  [data]
  #_{:clj-kondo/ignore [:unresolved-var]}
  (when data
    (let [inc-fn (fnil inc -1)
          kw-fn (fn [ident attr]
                  (let [ns (-> ident
                               str
                               (str/replace "/" "_")
                               (str/replace #"^:" ""))
                        n (-> attr
                              str
                              (str/replace "/" "_")
                              (str/replace #"^:" ""))]
                    (keyword ns n)))]
      (m/rewrite
       [data {:ident (keyword (gensym "_item_"))}]

       (m/and [(hash-set & ?elements) ?env]
              (m/let [?set (apply hash-set ?elements)]))
       ?set

       [(m/pred set? (m/seqable !elements ...)) ?env]
       (m/cata [(hash-set (m/cata [!elements ?env]) ...) ?env])

       (m/and [{::attr (m/some ?attr) ::value (m/some ?value)} {:ident ?id & ?env}]
              (m/let [?ident (kw-fn ?id ?attr)]))
       [?attr (m/cata [?value {:ident ?ident & ?env}])]

       [{::attr (m/some ?attr) ::value (m/pred nil?)} _]
       [?attr nil]

       (m/and [[?item] {:ident ?id}]
              (m/let [?m-idx (inc-fn nil)
                      ?ident (kw-fn ?id (str "item-" ?m-idx))]))
       [(m/cata [?item {:ident ?ident :idx ?m-idx}])]

       (m/and [[?item & ?rest] {:ident ?id :idx ?idx & ?env}]
              (m/let [?m-idx (inc-fn ?idx)
                      ?ident (kw-fn ?id (str "item-" ?m-idx))]))
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
       ?expr))))

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
      parse-strings
      parse-patterns
      component->entity))

(defn dataset [data & {:keys [id-attr ns] :or {id-attr :id ns "data"}}]
  (-> data
      (id->ident id-attr)
      parse-strings
      (kw->qkw ns)
      component->entity))

(def conn> engine/conn>)

(def rule-atom engine/rule-atom)

(def rules<
  (sh/|<= (map ::rules)))

(def q>< engine/q><)

(def query-rule>< engine/query-rule><)

(def entity>< engine/entity><)

(def entity< engine/entity<)

(def export< engine/export<)

(def stop sh/stop)

(defn export->! [engine !data]
  (-> engine
      export<
      (flow/<->! !data))
  engine)

(def import engine/import)

(pr/add-readers {'mutation (fn [mutation]
                             (let [{:keys [key params query]} (eql/expr->ast mutation)
                                   m {:mutation/call (keyword key)
                                      :mutation/params (kw->qkw params)}]
                               (if query
                                 (assoc m :mutation/query query)
                                 m)))
                 'ruleset (fn [ruleset]
                            (->> ruleset
                                 (mapv (fn [[id rule]]
                                         (assoc rule :id id)))
                                 ruleset))
                 'dataset (fn [dataset]
                            (dataset dataset))
                 #?@(:cljs ['json (fn [json]
                                    (-> (.parse js/JSON json)
                                        (js->clj :keywordize-keys true)))])})

(defn q! [engine q & args]
  (let [q! (::q! engine)]
    (if (fn? q!)
      (apply q! q args)
      (throw (ex-info "No q! function found in engine" {:engine engine})))))
