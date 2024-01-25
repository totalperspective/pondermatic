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

