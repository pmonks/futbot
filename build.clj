;
; Copyright © 2021 Peter Monks
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;
; SPDX-License-Identifier: Apache-2.0
;

(ns build
  "Build script for futbot.

For more information, run:

clojure -A:deps -T:build help/doc"
  (:require [clojure.string          :as s]
            [clojure.tools.build.api :as b]
            [org.corfield.build      :as bb]))

(def lib       'org.github.pmonks/fubot)
(def version   (format "1.0.%s" (.format (java.text.SimpleDateFormat. "yyyyMMdd") (java.util.Date.))))
(def uber-file "./target/futbot-standalone.jar")

; Utility fns
(defn set-opts
  [opts]
  (assoc opts
         :lib     lib
         :version version))

(defmulti exec "Executes the given command line, expressed as either a string, or a sequential (vector or list)." (fn [& args] (sequential? (first args))))

(defmethod exec true
  ([command-line] (exec command-line nil))
  ([command-line opts]
    (let [result (b/process (into {:command-args command-line} opts))]
      (if (not= 0 (:exit result))
        (throw (ex-info (str "Command '" (s/join " " command-line) "' failed (" (:exit result) ").") result))
        result))))

(defmethod exec false
  ([command-line] (exec command-line nil))
  ([command-line opts]
    (exec (s/split command-line #"\s+") opts)))

; Development-time tasks
(defn clean
  "Clean up the project."
  [opts]
  (bb/clean (set-opts opts)))

(defn uber
  "Create an uber jar."
  [opts]
  (-> opts
    (set-opts)
    (assoc :uber-file uber-file
           :main      'futbot.main)
    (bb/uber)))

(defn check
  "Check the code by compiling it."
  [opts]
  (bb/run-task (set-opts opts) [:check]))

(defn outdated
  "Check for outdated dependencies."
  [opts]
  (bb/run-task (set-opts opts) [:outdated]))

(defn kondo
  "Run the clj-kondo linter."
  [opts]
  (bb/run-task (set-opts opts) [:kondo]))

(defn eastwood
  "Run the eastwood linter."
  [opts]
  (bb/run-task (set-opts opts) [:eastwood]))

(defn lint
  "Run all linters."
  [opts]
  (-> opts
    (kondo)
    (eastwood)))

(defn ci
  "Run the CI pipeline."
  [opts]
  (-> opts
    (outdated)
    (check)
    (lint)))
