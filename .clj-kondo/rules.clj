(ns rules)

(defn ->rule [_])
(defn parse [_ _])

(defmacro ruleset
  "Returns a vector of rules after transforming the given map."
  [rules]
  (reduce
    (fn [v {:keys [rule-name fn-name conditions when-body then-body then-finally-body arg]}]
      (conj v `(->Rule ~rule-name
                       (mapv map->Condition ~conditions)
                       nil
                       ~(when (some? when-body) ;; need some? because it could be `false`
                          `(fn ~fn-name [~'session ~arg] ~when-body))
                       ~(when then-body
                          `(fn ~fn-name [~'session ~arg] ~@then-body))
                       ~(when then-finally-body
                          `(fn ~fn-name [~'session] ~@then-finally-body)))))
    []
    (mapv ->rule (parse ::rules rules))))
