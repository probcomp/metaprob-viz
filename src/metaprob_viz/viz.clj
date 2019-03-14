(ns metaprob-viz.viz
  (:require [org.httpkit.server :as httpkit]
            [clojure.core.async :as async :refer [chan alt!! timeout put! <!!]]
            [compojure.core :refer [defroutes GET ]]
            [compojure.route :refer [files not-found]]
            [clojure.string :as str]
            [ring.util.response :refer [file-response]]
            [cheshire.core :as json]))

(defonce port (atom 8081))
(defonce server (atom nil))
(defonce visualizations (atom {}))

(defn uuid [] (str (java.util.UUID/randomUUID)))
;;;;;;;;;;
;; websocket handling
;;;;;;;;;;
(defn handle-connect
  [{:keys [clientId vizId]} channel]
  (do
    (swap! visualizations assoc-in [vizId :clients clientId] channel)
    (httpkit/send! channel (json/generate-string
                            {:action "initialize"
                             :traces (get-in @visualizations [vizId :traces])
                             :info   (get-in @visualizations [vizId :info])}))))

(defn handle-disconnect [{:keys [clientId vizId]}]
  (swap! visualizations update-in [vizId :clients] dissoc clientId)
  (println @visualizations))

(defn handle-save [{:keys [vizId content]}]
  (swap! visualizations assoc-in [vizId :html] content)
  (println @visualizations))

(defn ws-handler
  "Ring handler for managing the web socket. Accept 'messages', dispatch
  to functions based on :action"
  [request]
  (httpkit/with-channel request channel
    (httpkit/on-receive channel
                        (fn [data]
                          (let [msg (json/parse-string data true)]
                            (do (println "Got over ws:" msg)
                                (case (:action msg)
                                  "connect" (handle-connect msg channel)
                                  "disconnect" (handle-disconnect msg)
                                  "save" (handle-save msg))))))))

;;;;;;;;;;
;; ring middleware
;;;;;;;;;;

(defn viz-disk-path
  "Given a visualization ID, return the path to its renderer"
  [v-id]
  (get-in @visualizations [v-id :path]))

(defn wrap-viz
  "Ring middleware for handling requests involving visualization IDs,
  which return static content based on the path of the
  visualzation. Requests for visualizations arrive looking like:

       http://localhost/<visualization-id>/

  and

      http://localhost/<visualization-id>/path/to/main.js

  Attempt to map `visualization-id` to a registered visualization. If
  we succeed, return the static HTML/js/CSS content being requested,
  rooted at the visualization's :path. If we fail to map the ID, call
  the wrapped handler.
  "
  [handler]
  (fn [{:keys [uri] :as req}]
    (let [path-elems   (rest (str/split uri #"/+"))
          viz-id       (first path-elems)
          viz-resource (if-let [res (seq (rest path-elems))]
                         (str/join "/" res)
                         "index.html")
          viz-path     (viz-disk-path viz-id)]

      (if (nil? viz-path)
        (handler req)
        (file-response viz-resource {:root viz-path})))))

(defn wrap-ws
  "Ring middleware to detect if the request is a websocket upgrade
  request, in which case we call our websocket handler. If not, we
  pass the request through unchanged."
  [handler ws-handler]
  (fn [req]
    (if (:websocket? req)
      (ws-handler req)
      (handler req))))

;; The bottom of our handler stack: serve static files, a placeholder
;; index page, 404's.
(defroutes routes
  (GET "/" [] "<html><body>Metaprob Viz Server. Hi!</body></html>")
  (files "/public")
  (not-found "<p>Page not found.</p>"))

;;;;;;;;;
;; API- functions to register visualizations, to add and remove traces
;;;;;;;;;

(defn add-viz!
  [path info]
  (let [id (uuid)]
    (swap! visualizations assoc id
           {:path path
            :clients {} ;; uuid -> ws
            :info info
            :traces {}  ;; uuid -> trace
            :id id
            :html nil})
    id))

(defn put-trace!
  [viz-id trace & [trace-id]]
  (let [trace-id (or trace-id (uuid))]
    (swap! visualizations assoc-in [viz-id :traces trace-id] trace)
    (doall (map (fn [channel]
                  (httpkit/send! channel (json/generate-string
                                          {:action "putTrace"
                                           :tId    trace-id
                                           :t      trace})))
                (vals (get-in @visualizations [viz-id :clients]))))
    trace-id))

(defn delete-trace!
  [viz-id trace-id]
  (do
    (swap! visualizations update-in [viz-id :traces] dissoc trace-id)
    (doall (map (fn [channel]
                  (httpkit/send! channel (json/generate-string
                                          {:action "removeTrace"
                                           :tId    trace-id})))
                (vals (get-in @visualizations [viz-id :clients]))))))

(defn client-save-html
  "Send a saveHTML message over the provided websocket. Return the provided viz ID"
  [c viz-id]

  (httpkit/send!
   c
   (json/generate-string
    {:action "saveHTML"}))

  viz-id)

(defn watch-timeout
  "Given an atom `a`, a function `f` over `a`, and a timeout in seconds,
  watch the value of `(f a)` for at most `t-o` seconds. If the value
  of `(f a)` is initially non-nil, or `a` changes and `(f a)` becomes
  non-nil within `t-o` seconds, return that non-nil value. If `(f a)`
  remain `nil` for `t-o` seconds, return `nil`. This function can
  block for `t-o` seconds."

  [a f t-o]
  (let [c            (chan)
        watcher      (keyword (gensym "watcher"))]

    (when-let [v (f @a)] (put! c v))

    (add-watch a
               watcher
               (fn [_ v _ _]
                 (when-let [v (f @a)]
                   (put! c v))))

    ((fn [[res _]]
       (remove-watch a watcher)
       res)
     (async/alts!! [c (timeout (* t-o 1000))]))))

(defn viz-client
  "Given a visualization ID, return a client socket connected to this
  visualization. If there are no clients yet, block for `timeout`
  seconds waiting for one to connect. Return nil if no clients are
  connected or connect within timeout seconds"
  [viz-id t-o]
  (let [get-a-client (fn [v] (-> v
                                 (get-in [viz-id :clients])
                                 vals
                                 seq
                                 first))]
    (watch-timeout visualizations get-a-client t-o)))

(defn viz-html
  "Given a visualization ID, returns the HTML for this visualization. If
  the HTML is not set yet, block for `timeout` seconds waiting for
  HTML to arrive. Return nil if no HTML arrives before `t-o` seconds."
  [viz-id t-o]
  (watch-timeout visualizations
                 (fn [v] (get-in v [viz-id :html]))
                 t-o))

(defn viz-url [v-id]
  (str "http://127.0.0.1:" @port "/" v-id "/"))

(defn get-html
  "Given a visualization ID, return the HTML from that
  visualization. Also takes a timeout: will wait for `timeout` seconds
  for a client to connect, and for a connected client to return
  HTML. If either timeout expires, returns `nil`"
  [viz-id & [timeout]]
  (some-> viz-id
          (viz-client (or timeout 5))
          (client-save-html viz-id)
          (viz-html (or timeout 5))))

(defn save-to-file [viz-id path]
  (spit path (get-html viz-id)))

(defn set-port! [p]
  (reset! port p))

(defn start-server! [ & [p]]
  (set-port! (or p 8081))
  (reset! server (httpkit/run-server
                  (-> #'routes
                      (wrap-ws ws-handler)
                      wrap-viz)
                  {:port @port})))

(defn stop-server! []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))
