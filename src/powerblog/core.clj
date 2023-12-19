(ns powerblog.core
  (:require [powerblog.ingest :as ingest]
            [powerblog.pages :as pages]))

(def config
  {:site/title "The Powerblog"
   :powerpack/render-page #'pages/render-page
   :powerpack/create-ingest-tx #'ingest/create-tx})
