(ns example
  (:require [metaprob-viz.viz :as viz]
            [clojure.java.browse :as browse]))

;; start visualization server.
(viz/start-server!)

;; declare a visualization, using the "dist" renderer, available at
;; the indicated path from the root of this repo.
(def vid (viz/add-viz! "public/vue/dist/" [[-2.0 -1.0    0 1.0 2.0]
                                           [-2.0 -1.0    0 1.0 2.0]]))

;; open empty visualization in browser
(browse/browse-url (str "http://localhost:8081/" vid "/"))

;; define a couple of example traces
(def trace-0 {:slope 1.4
              :intercept 0
              :inlier_std 0.2
              :outlier_std 1.2
              :outliers [false false true false true]})

(def trace-1 {:slope 0.5
              :intercept -1
              :inlier_std 0.1
              :outlier_std 2.1
              :outliers [false false true false true]})

;; add those traces to the visualization
;; if you view your browser now, you should see those visualizations
(def t0-id (viz/put-trace! vid trace-0))
(def t1-id (viz/put-trace! vid trace-1))

;; put-trace! takes an optional third argument- a trace ID. Reusing an
;; existing trace ID may lead the renderer to update the exisint trace
;; in place, enabling animation. This example will replace trace-1's
;; plot with trace-0's data:
(viz/put-trace! vid trace-0 t1-id)

;; and this will return the plot to trace-1's data:
(viz/put-trace! vid trace-1 t1-id)

;; fetch the static HTML of the visualization
(viz/get-html vid)

;; remove both traces
(viz/delete-trace! vid t0-id)
(viz/delete-trace! vid t1-id)

(viz/stop-server!)
