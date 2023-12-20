(ns powerblog.pages.blog-listing
  (:require [powerblog.layout :as layout]
            [powerblog.pages.frontpage :as frontpage]))

(defn render-page [context page]
  (layout/layout {:title [:i18n ::page-title]}
   [:article.prose.dark:prose-invert.mx-auto
    [:h1 [:i18n ::page-title]]
    [:ul
     (for [blog-post (frontpage/get-blog-posts (:app/db context))]
       [:li [:a {:href (:page/uri blog-post)} (:page/title blog-post)]])]]))
