## Metaprob visualization server

This implements the HTTP and websocket protocol first implemented for
[GenViz](https://github.com/probcomp/GenViz). Widgets / "renderers"
should be interchangeable between Metaprob-viz and GenViz; in fact,
the example included in this repo uses the Vue-based "dist" renderer
also used in the GenViz example.

### Usage

See `example.clj`, which should be usable in a REPL.

### Example notebook

Included is a Jupyter notebook and a Docker container in which to run
it, which demonstrates the use of metaprob-viz.

Build the container with `make docker-build`. When that completes, try
the notebook with `make docker-notebook`.

### API

#### viz/start-server! and stop-server!

    (start-server! 8081)
    (stop-server!)

Starts and stops a metaprob-viz server instance listening on
port 8081. This server manages HTTP and websocket traffic from the
client renderer.

#### viz/add-viz!

    (add-viz! path info)

Declare the existence of a visualization. Takes the on-disk path to a
renderer, and any arbitrary extra data the renderer will require for
displaying trace data later, specific for each renderer.

Returns the ID of the new visualization.

Example:

    (add-viz! "public/vue/dist/" [[-2.0 -1.0    0 1.0 2.0]
                                  [-2.0 -1.0    0 1.0 2.0]])

#### viz/put-trace!

    (put-trace! visualization-id trace-data & [trace-id])

Adds a trace data structure to the internal state, and send the trace
data to each client connected to the indicated visualization. Trace
data is, of course, renderer-specific.

The optional `trace-id` argument is used to replace an existing trace
in the visualization.

Returns the ID of the trace.

Example:

    (put-trace! "e48beec7-a952-4481-a473-a198c516c836"
      {:slope 1.4
       :intercept 0
       :inlier_std 0.2
       :outlier_std 1.2
       :outliers [false false true false true]})

#### viz/delete-trace!

    (delete-trace visualization-id trace-id)

Removes a trace from the internal state and from any connected clients.

#### viz/get-html

    (get-html! visualization-id & [timeout])

Returns the rendered HTML of the indicated visualization. Used to
create static content from visualizations. Be aware that a client is
used to generate the static HTML, so a client must be connected to the
visualization when this call is made, _or must connect within
`timeout` seconds_ (with a default of 5 seconds, if not given). This
call can block for up to `timeout` seconds.

Returns a string containing the generated HTML, or `nil` if the
visualization does not exist or we time out waiting for the client.

#### viz/save-to-file

    (save-to-file visualization-id path)

Saves the static HTML of the visualization at the path indicated (path
is relative to the root of the running Clojure process)

#### notebook/open-in-notebook

    (open-in-notebook visualization-id & [height])

Meant for use in Clojupyter notebook, this function, when evaluated in
a cell, will open a dynamic visualization in the notebook. It takes an
optional height argument, in pixels.

#### notebook/display-in-notebook

    (display-in-notebook visualization-id & [height])

Like `open-in-notebook`, this opens a visualization in the notebook,
but the visualization is static: it won't change even as the traces
which it shows change.
