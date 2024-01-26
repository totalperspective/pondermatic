(ns scratch.fns
  (:require [pondermatic.eval :refer [eval-string]]))

(eval-string (str '(->> ?length-max
                        inc
                        (range ?length-min)
                        (map (fn [l]
                               [l (str l (if (= 1 l) " month" " months"))]))
                        (map (partial str.join "|"))
                        (str.join "\n")))
             '{:bindings {?length-min  1
                          ?length-max 12}})


(eval-string (str '{prompt #expr (if ?single
                                   "I can confirm I am able to pay the deposit of £"
                                   "I can confirm we are able to pay the deposit of £")
                    deposit ?deposit}
                  '{:bindings {?single true
                               ?deposit 100}}))

(eval-string (str '(math.round 122)))
