(ns example
  (:require [metaprob-viz.viz :as viz]
            [clojure.java.browse :as browse]))

;; start visualization server
(viz/start-server!)

;; declare a visualization, using the "dist" visualization
(def vid (viz/add-viz! "public/vue/dist/" [[-2.0 -1.0    0 1.0 2.0]
                                           [-2.0 -1.0    0 1.0 2.0]]))

;; open empty visualization in browser
(browse/browse-url (str "http://localhost:8081/" vid "/"))

;; define a couple of example traces
(def t0 {:slope 1.4
         :intercept 0
         :inlier_std 0.2
         :outlier_std 1.2
         :outliers [false false true false true]})

(def t1 {:slope 0.5
         :intercept -1
         :inlier_std 0.1
         :outlier_std 2.1
         :outliers [false false true false true]})

;; add those traces to the visualization
;; if you view your browser now, you should see those visualizations
(def t0-id (viz/put-trace! vid t0))
(def t1-id (viz/put-trace! vid t1))

;; fetch the static HTML of the visualization
(viz/get-html vid 2)
