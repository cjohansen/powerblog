(ns powerblog.dev
  (:require [powerblog.core :as blog]
            [powerpack.dev :as dev]))

(defmethod dev/configure! :default []
  blog/config)

(comment

  (dev/start)
  (dev/stop)
  (dev/reset)

  (dev/get-app)

  )
