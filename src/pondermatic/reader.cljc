(ns pondermatic.reader
  (:require [clojure.edn :as edn]))

(def !readers
  (atom {'rule (fn [[when then]]
                 {:rule/when when
                  :rule/then then})}))

(defn add-readers [readers]
  (swap! !readers merge readers))

(defn -read-string [str]
  (edn/read-string {:readers @!readers
                    :default (fn [tag value]
                               {:reader.unknown/tag tag
                                :reader.unknown/value value})} str))