(ns powerblog.core
  (:require [m1p.core :as m1p]
            [powerblog.ingest :as ingest]
            [powerblog.pages :as pages]))

(defn pluralize [opt n & plurals]
  (-> (nth plurals (min (if (number? n) n 0) (dec (count plurals))))
      (m1p/interpolate-string {:n n} opt)))

(def config
  {:site/title "The Powerblog"
   :powerpack/render-page #'pages/render-page
   :powerpack/create-ingest-tx #'ingest/create-tx

   :optimus/bundles {"app.css"
                     {:public-dir "public"
                      :paths ["/styles.css"]}}

   :optimus/assets [{:public-dir "public"
                     :paths [#".*\.jpg"]}]

   :imagine/config {:prefix "image-assets"
                    :resource-path "public"
                    :disk-cache? true
                    :transformations
                    {:preview-small
                     {:transformations [[:fit {:width 184 :height 184}]
                                        [:crop {:preset :square}]]
                      :retina-optimized? true
                      :retina-quality 0.4
                      :width 184}}}

   :m1p/dictionaries {:nb ["src/powerblog/i18n/nb.edn"]
                      :en ["src/powerblog/i18n/en.edn"]}
   :m1p/dictionary-fns {:fn/plural #'pluralize}})
