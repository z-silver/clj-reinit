(ns user
  (:require
    [clj-reinit.core :as reinit :refer [stop]]
    [clj-reload.core :as reload :refer [reload]]
    [clojure.repl :refer :all]
    [init.discovery :as discovery]))

(def ^{:init/name ::reinit/config}
  reloader-config
  {:dirs ["dev" "src"]
   :auto-reload? true})

(defn config []
  ;; you'll want to setup your configuration here
  (discovery/from-namespaces [(the-ns 'user)]))

;; for convenience, so we don't have to call set-prep! separately
(defn go []
  (reinit/set-prep! #'config)
  (reinit/go))

(defn reset []
  (reinit/set-prep! #'config)
  (reinit/reset))

(defn hard-reset []
  (reinit/set-prep! #'config)
  (reinit/hard-reset))
