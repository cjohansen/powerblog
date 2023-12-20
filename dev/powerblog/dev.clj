(ns powerblog.dev
  (:require [datomic.api :as d]
            [powerblog.core :as blog]
            [powerpack.dev :as dev]))

(defmethod dev/configure! :default []
  blog/config)

(comment

  (set! *print-namespace-maps* false)

  (dev/start)
  (dev/stop)
  (dev/reset)

  (def app (dev/get-app))

  (require '[datomic.api :as d])

  (def db (d/db (:datomic/conn app)))

  (->> (d/entity db [:page/uri "/blog-posts/first-post/"])
       (into {}))

  (d/q '[:find ?uri
         :where
         [_ :page/uri ?uri]]
       db)

  (d/q '[:find [?tag ...]
         :where
         [_ :blog-post/tags ?tag]]
       db)

  )
