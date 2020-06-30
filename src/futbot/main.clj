;
; Copyright © 2020 Peter Monks
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

(ns futbot.main
  (:require [CLJ-2253]
            [futbot.config         :as cfg]
            [clojure.string        :as s]
            [clojure.java.io       :as io]
            [clojure.tools.cli     :as cli]
            [clojure.tools.logging :as log]
            [mount.core            :as mnt :refer [defstate]]
            [java-time             :as tm]
            [futbot.core           :as core])
  (:gen-class))

(def ^:private cli-opts
  [["-c" "--config-file FILE" "Path to configuration file (defaults to 'config.edn' in the classpath)"
    :validate [#(.exists (io/file %)) "Must exist"
               #(.isFile (io/file %)) "Must be a file"]]
   ["-h" "--help"]])

(defn usage
  [options-summary]
  (s/join
    \newline
    ["Runs the futbot Discord bot."
     ""
     "Usage: futbot [options]"
     ""
     "Options:"
     options-summary
     ""]))

(defn- error-message
  [errors]
  (str "The following errors occurred while parsing the command line:\n\n"
       (s/join \newline errors)))

(defn- exit
  [status-code message]
  (println message)
  (System/exit status-code))

(defn -main
  "Runs futbot."
  [& args]
  (try
    (log/info "Starting futbot on Clojure" (clojure-version) "/ JVM" (System/getProperty "java.version"))
    (log/info "Built at" (tm/format :iso-instant cfg/built-at) "from" cfg/git-url)
    (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-opts)]
      (cond
        (:help options) (exit 0 (usage summary))
        errors          (exit 1 (error-message errors)))

      ; Start the bot
      (mnt/with-args options)
      (mnt/start)
      (core/start-bot!))  ; This must go last, as it blocks
    (catch Exception e
      (log/error e)
      (System/exit -1))))
