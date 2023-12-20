(ns powerblog.ingest
  (:require [datomic.api :as d]))

(defn get-page-kind [file-name]
  (cond
    (re-find #"^blog-posts/" file-name)
    :page.kind/blog-post

    (re-find #"^index(-nb)?\.md" file-name)
    :page.kind/frontpage

    (re-find #"\.md$" file-name)
    :page.kind/article))

(defn create-tx [file-name txes]
  (let [kind (get-page-kind file-name)]
    (for [tx txes]
      (cond-> tx
        (and (:page/uri tx) kind)
        (assoc :page/kind kind)))))

(defn on-ingested [powerpack-app results]
  (->> (for [tag (d/q '[:find [?tag ...]
                        :where
                        [_ :blog-post/tags ?tag]]
                      (d/db (:datomic/conn powerpack-app)))]
         {:page/uri (str "/tag/" (name tag) "/")
          :page/kind :page.kind/tag
          :tag-page/tag tag})
       (d/transact (:datomic/conn powerpack-app))
       deref))
