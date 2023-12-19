(ns powerblog.core)

(defn render-page [context page]
  "<h1>Hello world</h1>")

(def config
  {:site/title "The Powerblog"
   :powerpack/render-page #'render-page})
