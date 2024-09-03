(ns pondermatic.portal.source-map
  (:require ["source-map" :refer [SourceMapConsumer]]
            [clojure.string :as str]
            [kitchen-async.promise :as p]
            #_[clojure.pprint :refer [pprint]]))

(def fs (try (js/require "fs")
             (catch js/Error _ nil)))

(def source-files! (atom {}))
(def source-maps! (atom {}))

(def source-location-regex #" \(?([^ ]+)\)?:(\d+):(\d+)\)?")

(def extract-source-location
  (partial re-find source-location-regex))

(def parse-position
  (comp clj->js (partial zipmap [:source :line :column]) rest))

(defn find-source-file
  ([source-file]
   (find-source-file source-file source-file))
  ([source-file root-file]
   (if-not fs
     root-file
     (if (contains? @source-files! source-file)
       (get @source-files! source-file)
       (if (fs.existsSync source-file)
         (do
           #_(pprint {:phase "find-source-file"
                      :step "found source file"
                      :root-file root-file
                      :source-file source-file})
           (swap! source-files! assoc root-file source-file)
           source-file)
         (let [path-elements (str/split source-file #"/")
               source-path (str/join "/" (butlast path-elements))
               source-file (str source-path "." (last path-elements))]
           #_(pprint {:phase "find-source-file"
                      :step "source-path"
                      :source-file source-file
                      :source-path source-path})
           (if (seq source-path)
             (find-source-file source-file root-file)
             (do
               #_(pprint {:phase "find-source-file"
                          :step "no source path"
                          :source-file source-file
                          :root-file root-file})
               (swap! source-files! assoc root-file nil)
               nil))))))))

(defn async-load-file [file]
  (if-not fs
    (js/fetch file)
    (fs.promise.readFile file "utf8")))

(defn fetch-source-map [source-file]
  (if-not source-file
    true
    (if-let [source-map (get @source-maps! source-file)]
      (p/do source-map)
      (when-let [source-file (find-source-file source-file)]
        (let [map-file (str source-file ".map")]
          (p/try
            (p/let [data (async-load-file map-file)
                    source-map (js/JSON.parse data)
                    consumer (SourceMapConsumer. source-map)]
              #_(pprint {:phase "fetch-source-map"
                         :step "load map file"
                         :map-file map-file
                         :data-size (count data)})
              (swap! source-maps! assoc source-file consumer)
              consumer)
            (p/catch js/Error e
              (if (str/includes? (.-message e) "no such file")
                (do
                  #_(pprint {:phase "fetch-source-map"
                             :step "no such file"
                             :map-file source-file})
                  nil)
                (do
                  #_(pprint {:phase "fetch-source-map"
                             :step "error fetching source map"
                             :map-file map-file
                             :error e})
                  nil)))))))))

(defn find-source-maps [stack-trace]
  (->> stack-trace
       str/split-lines
       (keep extract-source-location)
       (map second)
       distinct
       (run! fetch-source-map)))

(defn apply-source-map-to-line [line]
  (if-let [match (extract-source-location line)]
    (p/let [position (parse-position match)
            consumer (get @source-maps! (.-source position))]
      (if consumer
        (p/let [original-position (.originalPositionFor ^SourceMapConsumer consumer position)
                mapped-source (str " " (.-source ^string original-position) ":"
                                   (.-line ^string original-position) ":"
                                   (.-column ^string original-position))
                new-line (str/replace line (first match) mapped-source)]
          new-line)
        line))
    line))

(defn async-join-lines [lines]
  (p/let [processed-lines (p/all lines)]
    (str/join "\n" processed-lines)))

(defn apply-source-maps-to-stack-trace [stack-trace]
  (->> stack-trace
       str/split-lines
       (map apply-source-map-to-line)
       async-join-lines))

(defn remove-path-prefix [line]
  (str/replace line source-location-regex
               (fn [[_ file line column]]
                 (let [simplified-file (str/replace file #".+?cljs-runtime/" "")]
                   (str " " simplified-file ":" line ":" column)))))

(defn apply-source-maps [stack-trace]
  (p/let [_ (find-source-maps stack-trace)
          mapped-stack-trace (apply-source-maps-to-stack-trace stack-trace)
          processed-stack-trace (str/join "\n" (map remove-path-prefix (str/split-lines mapped-stack-trace)))]
    processed-stack-trace))


;; Do not alter
(comment
  (def stack-trace "Error: No protocol method IDeref.-deref defined for type null: \n    at Object.cljs.core/missing-protocol [as missing_protocol] (/Users/bahulneel/Projects/TotalPerspective/pondermatic/.shadow-cljs/builds/dev/dev/out/cljs-runtime/cljs/core.cljs:324:3)\n    at cljs$core$IDeref$_deref$dyn (/Users/bahulneel/Projects/TotalPerspective/pondermatic/.shadow-cljs/builds/dev/dev/out/cljs-runtime/cljs/core.cljs:690:1)\n    at Object.cljs.core/-deref [as _deref] (/Users/bahulneel/Projects/TotalPerspective/pondermatic/.shadow-cljs/builds/dev/dev/out/cljs-runtime/cljs/core.cljs:690:1)\n    at Object.cljs.core/deref [as deref] (/Users/bahulneel/Projects/TotalPerspective/pondermatic/.shadow-cljs/builds/dev/dev/out/cljs-runtime/pondermatic/engine.cljc:45:5)\n    at Function.fexpr__69242 [as cljs$core$IFn$_invoke$arity$1] (/Users/bahulneel/Projects/TotalPerspective/pondermatic/.shadow-cljs/builds/dev/dev/out/cljs-runtime/pondermatic/shell.cljc:50:17)\n    at /Users/bahulneel/Projects/TotalPerspective/pondermatic/.shadow-cljs/builds/dev/dev/out/cljs-runtime/pondermatic/shell.cljc:19:34\n    at fexpr__64782 (/Users/bahulneel/Projects/TotalPerspective/pondermatic/.shadow-cljs/builds/dev/dev/out/cljs-runtime/pondermatic.shell.js:50:3)\n    at /Users/bahulneel/Projects/TotalPerspective/pondermatic/.shadow-cljs/builds/dev/dev/out/cljs-runtime/cloroutine/impl.cljc:60:19\n    at Function.G__66131__0 [as cljs$core$IFn$_invoke$arity$0] (/Users/bahulneel/Projects/TotalPerspective/pondermatic/.shadow-cljs/builds/dev/dev/out/cljs-runtime/cloroutine.impl.js:45:3)")



  (p/then (p/try
            (p/let [result (apply-source-maps stack-trace)]
              result)
            (p/catch js/Error e
              (js/console.error e)
              nil))
          (fn [result]
            (println "Result\n" result))))
