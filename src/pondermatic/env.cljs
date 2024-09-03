(ns pondermatic.env)

(def node?
  "Check if we're in a Node.js environment"
  (and (exists? js/process)
       (-> js/process .-release .-name (= "node"))))

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