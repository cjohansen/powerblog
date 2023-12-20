(ns powerblog.pages.tag
  (:require [datomic.api :as d]
            [powerblog.layout :as layout]))

(defn get-blog-posts [db tag]
  (->> (d/q '[:find [?e ...]
              :in $ ?tag
              :where
              [?e :blog-post/tags ?tag]]
            db tag)
       (map #(d/entity db %))))

(defn render-page [context page]
  (let [title (str "Blog posts about " (name (:tag-page/tag page)))]
    (layout/layout
     {:title title}
     [:article.prose.dark:prose-invert.mx-auto
      [:h1 title]
      [:ul
       (for [blog-post (get-blog-posts (:app/db context) (:tag-page/tag page))]
         [:li [:a {:href (:page/uri blog-post)} (:page/title blog-post)]])]])))
