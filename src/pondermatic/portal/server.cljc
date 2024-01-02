(ns pondermatic.portal.server
  (:require [portal.api :as p]))

(def port 5678)

(defn -main [& _]
  (let [p (p/open {:port port})]
    (println "Portal open on " (p/url p))))
