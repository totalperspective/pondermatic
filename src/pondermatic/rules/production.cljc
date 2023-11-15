(ns pondermatic.rules.production
  (:require [hyperfiddle.rcf :refer [tests tap %]]))

(tests
 (tap 1)
 % := 1)
