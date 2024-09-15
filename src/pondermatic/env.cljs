(ns pondermatic.env
  (:require ["detect-node" :as is-node]))

(def node?
  "Check if we're in a Node.js environment"
  (try
    (is-node)
    (catch js/Error e
      false)))

(def browser?
  "Check if we're in a browser environment"
  (and (exists? js/window)
       (exists? js/document)))

(def web-worker?
  "Check if we're in a Web Worker environment"
  (and (not node?)
       (not browser?)
       (exists? js/self)
       (exists? js/postMessage)))
