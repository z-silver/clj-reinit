(ns clj-reinit.core
  (:require
    [clj-reload.core :as reload]
    [clojure.java.io :as io]
    [init.config :as config]
    [init.core :as init]
    [init.discovery :as discovery]
    [juxt.dirwatch :as dirwatch]))

(defonce system (agent nil))
(defonce ^:private prepare (atom nil))

(defn set-prep!
  "Sets the prepare symbol. This symbol must name a fn of no argumnets
  that returns a system configuration.

  Accepts a var, qualified symbol or qualified keyword as its argument."
  [prep]
  {:pre [(or (var? prep)
           (qualified-symbol? prep)
           (qualified-keyword? prep))]}
  (reset! prepare (symbol prep)))

(defn get-prep
  "Retrieves the prepare symbol. Asserts that it is, in fact, a symbol."
  []
  (let [prep @prepare]
    (assert (qualified-symbol? prep)
      "set-prep! has not been called")
    prep))

(defn clear-prep!
  "Clears the prepare symbol."
  []
  (reset! prepare nil))

(defn show-error
  "Shows the error in the system agent, if there is one.

  Use this function if something goes wrong in a reload to see
  what is going on."
  []
  (agent-error system))

(defn clear-error
  "Clears the error in the system agent, if there is one."
  []
  (restart-agent system @system))

(defmacro assert-errorless []
  `(assert (nil? (show-error))
     "error in system agent must be cleared"))

(defn stop
  "Stops the system. Idempotent."
  []
  (assert-errorless)
  (send system
    #(some-> % init/stop))
  :stopping)

(def default-config {})

(declare reloader auto-reloader)

(defn go
  "Starts the system. Idempotent, provided that the prepare symbol names an
  idempotent function.

  Injects the reloader and auto-reloader components into system before starting.
  If a config for them is not provided, also injects a default configuration."
  []
  (assert-errorless)
  (let [prep (get-prep)
        config* (discovery/bind ((find-var prep))
                  {::reloader #'reloader
                   ::auto-reloader #'auto-reloader})
        config (if (first (config/select config* ::config))
                 config*
                 (discovery/bind config* {::config #'default-config}))]
    (send system #(or % (init/start config))))
  :starting)

(defn reset
  "Stops the system.
  Reloads any changed namespaces.
  Enters the new version of the current namespace, if possible.
  Restarts the system by calling go.

  Use it from the REPL to ensure your namespace and running system are the
  latest version."
  []
  (assert-errorless)
  (get-prep)
  (stop)
  (reload/reload)
  (try (in-ns (ns-name *ns*))
    (catch Exception _))
  ((find-var `go)))

(defn hard-reset
  "Same as reset, except it additionally calls a user-supplied ::clear-state
  component function. The expected use is that the supplied function wipes
  any persistent state that would otherwise survive system restarts."
  []
  (assert-errorless)
  (get-prep)
  (some-> @system ::clear-state ((fn [f] (f))))
  (reset))

(defn reloader
  "Init component for clj-reload.

  Requires a ::config component containing a map suitable for passing to
  clj-reload's init function."
  {:init/inject [::config]}
  [config]
  (reload/init config)
  :ok)

(defn auto-reloader
  "Automatically reloads code when editing, using the same configuration
  as clj-reload.core/init. In particular, it cares about the :dirs key
  and the :files key. It additionally cares about the :auto-reload? key.

  Does not update the REPL namespace, use reset for that.

  Does nothing if :auto-reload? is falsey."
  {:init/stop-fn #'dirwatch/close-watcher
   :init/inject [::config ::reloader]}
  [{:keys [dirs files auto-reload?]} _]
  (assert (or (not files) (instance? java.util.regex.Pattern files))
    ":files must be either falsey or a regex")
  (apply dirwatch/watch-dir
    (fn [v]
      (when (re-matches (or files #".*\.cljc?")
              (-> v :file .getCanonicalPath))
        (reset)))
    (when auto-reload?
      (map io/file (or dirs (reload/classpath-dirs))))))
