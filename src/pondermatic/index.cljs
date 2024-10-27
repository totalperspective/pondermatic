(ns pondermatic.index
  (:require [pondermatic.core :as p]
            [pondermatic.rules :as r]
            [pondermatic.flow :as flow]
            [pondermatic.portal.utils :as p.util]
            [pondermatic.rules.production :as prp]
            [clojure.walk :as w]
            [cljs.pprint :as pp]
            [pondermatic.portal.client :as portal]
            [portal.console :as log]
            [promesa.core :as pa]
            [pondermatic.browser.client :as webc]
            [pondermatic.eval :as pe]
            [pondermatic.reader :refer [-read-string]]
            [pondermatic.browser.engine :as webe]
            [pondermatic.pool :as pool]
            [pondermatic.data :refer [uuid-hash] :as data]
            [pondermatic.env :as env]
            [pondermatic.shell :as sh])
  (:require-macros [pondermatic.macros :refer [|-><]]))

(defonce pool (-> {}
                  (webe/contructor :engine p/->engine p/clone>)
                  pool/->pool))

(def -to-pool! (partial webe/to-pool! pool))

(defn to-pool! [{:keys [::id ::engine]} & args]
  (cond
    engine (apply sh/|> engine args)
    id (apply -to-pool! id args)
    :else (throw (ex-info "No engine or id provided" {:args args}))))

(def with-agent< (partial pool/with-agent< pool))

(def -q>< (with-agent< webe/q><))

(defn q>< [{:keys [::id ::engine]} args]
  (cond
    engine (apply p/q>< engine args)
    id (-q>< id args)
    :else (throw (ex-info "No engine or id provided" {:args args}))))

(def -entity>< (with-agent< webe/entity><))

(defn entity>< [{:keys [::id ::engine]} & args]
  (cond
    engine (apply p/entity>< engine args)
    id (apply -entity>< id args)
    :else (throw (ex-info "No engine or id provided" {:args args}))))

(defn q! [{:keys [::id ::engine] :as agent} q & args]
  (let [q (-read-string q)
        args (js->clj args :keywordize-keys true)]
    (cond
      engine (clj->js (apply p/q! engine q args))
      id (throw (ex-info "q! not supported on pooled engine" {:agent agent}))
      :else (throw (ex-info "No engine or id provided" {:agent agent})))))

(def -entity< (with-agent< webe/entity<))

(defn entity< [{:keys [::id ::engine]} args]
  (cond
    engine (apply p/entity< engine args)
    id (apply -entity< id args)
    :else (throw (ex-info "No engine or id provided" {:args args}))))

(defn watch-agents [cb]
  (let [id (str (random-uuid))]
    (add-watch webc/!agents id (fn [_ new _]
                                 (cb (clj->js new))))
    id))

(defn remove-agents-watch [id]
  (remove-watch webc/!agents id))

(defn portal
  ([]
   (portal nil))
  ([launcher]
   (portal/start (when launcher
                   (keyword launcher)))))

(defn hash-id [js-obj]
  (-> js-obj
      (js->clj :keywordize-keys true)
      uuid-hash
      str))

(defn create-engine
  ([name]
   (create-engine name false))
  ([name reset-db?]
   (create-engine name reset-db? false))
  ([name reset-db? pool?]
   (if pool?
     (let [id (pool/add-agent! pool :engine name :reset-db? reset-db?)]
       {::id id})
     {::engine (p/->engine name :reset-db? reset-db?)})))

(defn stop [{::keys [::id ::engine]}]
  (cond
    engine (p/stop engine)
    id (pool/remove-agent! pool id)
    :else (throw (ex-info "No engine or id provided" {}))))

(defn ->edn [form]
  (let [visit-node (fn [node]
                     (if (map-entry? node)
                       (let [[attr val] node]
                         (if (symbol? attr)
                           [(keyword (str attr)) val]
                           [attr val]))
                       node))]
    (->> form
         str
         -read-string
         (w/postwalk visit-node))))

(defn parse-rule [rule]
  (-> rule
      (update :rule/when ->edn)
      (update :rule/then ->edn)))

(defn parse-rules [rules]
  (mapv parse-rule rules))

(defn ruleset [ruleset]
  (-> (if (string? ruleset)
        (-read-string ruleset)
        ruleset)
      (js->clj :keywordize-keys true)
      parse-rules
      p/ruleset
      (p.util/trace 'ruleset)))

(defn dataset [dataset]
  (-> (if (string? dataset)
        (-read-string dataset)
        dataset)
      (js->clj :keywordize-keys true)
      p/dataset
      (p.util/trace 'dataset)))

(defn wrap-query [query-fn]
  (if (fn? query-fn)
    (fn [q & args]
      (let [result (apply query-fn (-read-string q) args)]
        (clj->js result)))
    query-fn))

(defn sh [{:keys [::id ::engine] :as agent} msg]
  (log/info {:agent agent :msg msg})
  (let [msg (-> msg
                (js->clj :keywordize-keys true)
                (update :cb (fn [cb]
                              (when cb
                                (fn [x]
                                  (cb (clj->js (update x :query wrap-query)))))))
                (p.util/trace 'sh))]
    (cond
      engine (sh/|> engine msg)
      id (pool/to-agent! pool id msg)
      :else (throw (ex-info "No engine or id provided" {:msg msg})))))

(defn wrap-callbacks [msg]
  (w/postwalk (fn [node]
                (if (fn? node)
                  (fn [x]
                    (-> x clj->js node))
                  node))
              msg))

(defn cmd [msg]
  (to-pool! (-> msg
                (js->clj :keywordize-keys true)
                wrap-callbacks
                (p.util/trace 'cmd)))
  nil)

(defn copy [{:keys [::id]}]
  (when-not id
    (throw (ex-info "Not a pooled engine" {})))
  {::id (pool/copy-agent! pool id)})

(defn add-rules-msg [rules]
  (r/add-rules (-> rules
                   (js->clj :keywordize-keys true)
                   (p.util/trace 'add-rules-msg))))

(defn q [engine q args cb]
  (log/info {:msg "Executing query"
             :args {:id (::id engine) :q q :args args}})
  (let [args (js->clj args)
        q-args (-> q
                   -read-string
                   vector
                   (into args)
                   (p.util/trace ::parsed-query))
        <>q (q>< engine q-args)
        query-cb #(do
                    (log/info {:msg "Query result"
                               :result %})
                    (cb (clj->js %)))]
    (|->< <>q
          (flow/drain-using {::flow :query ::query q}
                            (flow/tapper query-cb)))))

(defn entity [engine ident cb]
  (log/trace {:entity/ident ident})
  (let [ident (-> ident
                  js->clj
                  str
                  -read-string)
        <entity (entity< engine [ident true])
        entity-cb (fn [entity]
                    (log/trace entity)
                    (let [entity (assoc entity :id (str ident))]
                      (log/trace {:ident ident
                                  :entity' entity})
                      (cb (clj->js entity))))]
    (<entity entity-cb #(cb nil %))))

(defn entity* [engine ident cb]
  (entity engine ident cb)
  (let [ident (-> ident
                  js->clj
                  str
                  -read-string)
        <>entity (entity>< engine [ident true])
        entity-cb (fn [entity']
                    (let [entity (when entity' (assoc entity' :id (str ident)))]
                      (log/trace {:ident ident
                                  :entity' entity})
                      (cb (clj->js entity))))]
    (|->< <>entity
          (flow/drain-using {::flow :entity ::ident ident} (flow/tapper entity-cb)))))

(defn basis-t [{:keys [::id ::engine]} cb]
  (let [<>t (cond
              engine (p/t>< engine)
              id (pool/to-agent! pool id (p/t>< engine))
              :else (throw (ex-info "No engine or id provided" {})))]
    (|->< <>t
          (flow/drain-using {::flow :basis-t} (flow/tapper cb)))))

(defn dispose! [task]
  (task))

(defn error-info [e]
  (-> e
      ex-data
      clj->js))

(defn ->promise-fn [cb-fn]
  (fn [& args]
    (let [p (pa/deferred)
          dispose! (atom (constantly nil))
          args (conj (vec args)
                     (fn
                       ([result]
                        (@dispose!)
                        (pa/resolve! p result))
                       ([_ e]
                        (@dispose!)
                        (pa/reject! p e))))]
      (reset! dispose! (apply cb-fn args))
      p)))

(defn unify [expr-or-str env]
  (let [expr (if (string? expr-or-str)
               (-read-string expr-or-str)
               (js->clj expr-or-str))
        env (js->clj env)
        [env return] (if (sequential? env)
                       [env vec]
                       [[env] first])
        env (->> env
                 (map #(reduce-kv (fn [m k v]
                                    (let [k (if (= \? (first k))
                                              (symbol k)
                                              k)]
                                      (assoc m k v)))
                                  {}
                                  %))
                 return)]
    (try
      (let [result (prp/unify-pattern expr env)]
        (log/trace {:unify/expr expr
                    :unify/env env
                    :unify/result result})
        (clj->js result))
      (catch js/Error e
        (log/error (ex-info "Failed to unify"
                            {:unify/expr expr
                             :unify/env env}
                            e))
        (throw e)))))

(defn log
  ([expr]
   (log nil expr))
  ([level expr]
   (let [expr (if (instance? js/Error expr)
                expr
                (js->clj expr :keywordize-keys true))]
     (condp = (keyword level)
       :debug (log/debug expr)
       :trace (log/trace expr)
       :info (log/info expr)
       :warn (log/warn expr)
       :error (log/error expr)
       :fatal (log/fatal expr)
       (if (instance? js/Error expr)
         (log/error expr)
         (log/log expr))))))

(defn parse-opts [node]
  (w/postwalk (fn [node]
                (if (map-entry? node)
                  (let [[key val] node]
                    [(-read-string (str key)) val])
                  node))
              (js->clj node)))

(defn js? [x]
  (or (number? x)
      (string? x)
      (array? x)
      (object? x)
      (instance? js/Error x)
      (= \# (first (pr-str (type x))))))

(defn format-obj [obj]
  (if (and (map? obj) (:level obj))
    (clj->js [:div {}
              [:div (pr-str (dissoc obj :result :form))]
              [:div (str (pp/write (:result obj) :stream nil))]])
    (clj->js [:div {} (str "clj: " (pp/write obj :stream nil))])))

(def devtoolsFormatter
  #js {:header (fn [obj _config]
                 (cond
                   (coll? obj) (format-obj obj)

                   (not (js? obj)) (format-obj obj)

                   :else nil))
       :hasBody (constantly true)
       :body (fn [obj _config]
               (clj->js [:object {:object obj}]))})

(defn toJS [form]
  (->> form
       js->clj
       (w/postwalk (fn [node]
                     (if (-> node
                             pr-str
                             first
                             (= \#))
                       (str node)
                       node)))
       clj->js))

(defn console-tap [x]
  (js/console.log (pr-str x)))

(defn raw-tap [x]
  (js/console.log x))

(defn eval-string
  ([str]
   (eval-string str false))
  ([str ->js?]
   (eval-string str ->js? {}))
  ([str ->js? opts]
   (let [res (pe/eval-string str
                             {:throw? true
                              :bindings
                              (-> opts
                                  parse-opts
                                  (assoc 'js? js?))})]
     (if ->js?
       (clj->js res)
       res))))

(def ^:private api
  #js {:createEngine create-engine
       :copy copy
       :ruleset ruleset
       :dataset dataset
       :sh sh
       :cmd cmd
       :addRulesMsg add-rules-msg
       :basisT basis-t
       :q$ q!
       :q q
       :qP (->promise-fn q)
       :entity entity
       :entityP (->promise-fn entity)
       :watchEntity entity*
       :hashId hash-id
       :errorInfo error-info
       :portal portal
       :dispose dispose!
       :log log
       :unify unify
       :pprint #(-> % js->clj pp/pprint)
       :addTap (fn
                 ([] (add-tap console-tap))
                 ([tap] (cond (fn? tap) (add-tap #(tap %))
                              :else (add-tap raw-tap))))
       :readString -read-string
       :toString pr-str
       :encode data/transit-json-writer
       :decode data/read-transit
       :eval eval-string
       :toJS toJS
       :devtoolsFormatter devtoolsFormatter
       :stop stop
       :watchAgents watch-agents
       :removeAgentsWatch remove-agents-watch})

(defn- init-api [api]
  (if env/web-worker?
    (do
      (js/console.log "Pondermatic - Web Worker detected - Initializing")
      (set! js/self.pondermatic #js {:api api})
      api)
    (do
      (js/console.log "Pondermatic - Initializing API")
      api)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exports []
  (cond
    ;; Browser environment with Pondermatic already initialized
    (and env/browser?
         (.-pondermatic js/window)
         (.-api js/window.pondermatic))
    (do
      (js/console.log "Pondermatic - Browser version detected")
      js/window.pondermatic.api)

    ;; Node.js environment
    env/node?
    (do
      (js/console.log "Pondermatic - Node.js environment detected")
      (init-api api))

    ;; Web Worker or other environment
    :else
    (init-api api)))
