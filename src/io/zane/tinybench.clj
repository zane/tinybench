(ns io.zane.tinybench
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [clojure.tools.cli :as cli]
            [criterium.core :as criterium]))

(def cli-options
  [["-c" "--config CONFIG"]
   ["-o" "--out OUT"]])

(defn require-namespaces
  [namespaces]
  (println "Requiring namespaces...")
  (doseq [ns namespaces]
    (print "  " ns "...")
    (require ns)
    (println "DONE")))

(defn run-benchmark
  [f-sym f args]
  (print "Running benchmark ")
  (pr (apply list f-sym args))
  (print "...")
  (flush)
  (let [result (criterium/quick-benchmark (apply f args) {})]
    (println "DONE")
    (flush)
    result))

(def config-entry-namespace
  (comp symbol namespace :fn))

(defn function
  [config-entry]
  (ns-resolve (config-entry-namespace config-entry)
              (:fn config-entry)))

(defn namespaces
  [config]
  (into #{}
        (map namespace)
        config))

(defn -main
  "Parses command-line options and uses them "
  [& args]
  (let [{{config-path :config out-path :out} :options} (cli/parse-opts args cli-options)
        config (edn/read-string (slurp config-path))]
    (require-namespaces (namespaces config))
    (let [plan (map (juxt :fn function :args) config)
          results (map (partial apply run-benchmark) plan)
          out (mapv (fn [config-entry result]
                      (assoc config-entry :result result))
                    config
                    results)]
      (spit out-path (with-out-str (pprint/pprint out))))))
