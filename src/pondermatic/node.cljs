(ns pondermatic.node
  (:require [pondermatic.portal.api :as p]))

(def exports #js {:startPortal p/start})
