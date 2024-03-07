(ns scratch.fns
  #_{:clj-kondo/ignore [:unused-namespace]}
  (:require [pondermatic.eval :refer [eval-string]]
            [clojure.string :as s]))

(eval-string '(->> ?length-max
                   inc
                   (range ?length-min)
                   (map (fn [l]
                          [l (str l (if (= 1 l) " month" " months"))]))
                   (map (partial str.join "|"))
                   (str.join "\n"))
             '{:bindings {?length-min  1
                          ?length-max 12}})


(eval-string '{prompt #expr (if ?single
                              "I can confirm I am able to pay the deposit of £"
                              "I can confirm we are able to pay the deposit of £")
               deposit ?deposit}
             '{:bindings {?single true
                          ?deposit 100}})

(eval-string '(math.round 122))

(let [f (eval-string
         (str '(fn [provides]
                 (->> provides
                      read-string
                      (w.postwalk
                       (fn [node]
                         (cond
                           (map-entry? node)
                           (let [[k v] node]
                             (if (and (vector? v) (= '$ (first v)))
                               (let [sym (->> k
                                              name
                                              gensym
                                              (str "?")
                                              symbol)]
                                 [k sym])
                               [k v]))

                           (and (vector? node)
                                (= '$ (first node))
                                (list? (second node)))
                           (let [[_ expr] node
                                 expr (apply hash-set expr)]
                             (if (contains? expr '?answer)
                               '?answer
                               node))
                           :else
                           node)))
                      pr-str)))
         {:bindings {'prn prn}})
      expr "{id ?answers
 seeker_profile/occupations
 [{id #expr (random-uuid)
   seeker_profile_occupations/occupation ?answer}]}"]
  (f expr))

