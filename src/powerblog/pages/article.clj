(ns powerblog.pages.article
  (:require [powerblog.layout :as layout]
            [powerpack.markdown :as md]))

(defn render-page [context page]
  (layout/layout {}
   layout/header
   [:article.prose.dark:prose-invert.mx-auto
    (md/render-html (:page/body page))]))
