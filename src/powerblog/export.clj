(ns powerblog.export
  (:require [powerblog.core :as blog]
            [powerpack.export :as export]))

(defn ^:export export! [& _args]
  (-> blog/config
      (assoc :site/base-url "https://www.example.com")
      export/export!))
