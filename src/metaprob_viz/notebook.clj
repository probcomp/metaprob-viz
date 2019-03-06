(ns metaprob-viz.notebook
  (:require [metaprob-viz.viz :as viz]
            [clojure.java.browse :as browse]
            [clojupyter.misc.display :as display]))

(defn open-in-notebook [v-id & [h]]
  (let [height (or h 600)]
    (display/hiccup-html
     [:iframe {:src (viz/viz-url v-id)
               :frameBorder 0
               :width "100%"
               :height height}])))

(defn open-in-tab [v-id]
  (display/hiccup-html
   [:a {:href (viz/viz-url v-id)
        :target "_blank"}
    "Click to open visualization window"]))

(defn display-in-notebook [f v-id & [h]]
  ;; (browse/browse-url (viz/viz-url v-id))
  ;; (open-in-notebook v-id h)
  (f)
  (display/html (viz/get-html v-id)))
