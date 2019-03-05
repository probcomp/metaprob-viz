(ns metaprob-viz.notebook
  (:require [metaprob-viz.viz :as viz]
            [clojupyter.misc.display :as display]))

(defn open-in-notebook [v-id & [h]]
  (let [height (or h 600)]
    (display/hiccup-html
     [:iframe {:src (viz/viz-url v-id)
               :frameBorder 0
               :width "100%"
               :height height}])))

(defn display-in-notebook [f v-id & [h]]
  (open-in-notebook v-id h)
  (display/html (viz/get-html v-id)))
