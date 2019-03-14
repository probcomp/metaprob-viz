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

(defn open-in-popup [v-id]
  (display/hiccup-html
   [:a {:href (viz/viz-url v-id)
        :onclick (str "window.open(this.href,
                                   'vizWindow-" v-id "',
                                   'toolbar=no, location=no, status=no, menubar=no, scrollbars=yes, resizable=yes'); return false;")}
    "<h4>Click to open visualization window</h4>"]))

(defn display-in-notebook [v-id]
  (display/html (viz/get-html v-id)))
