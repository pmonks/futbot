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

(ns run
  "Run script for futbot.

For more information, run:

clojure -A:deps -T:run help/doc"
  (:require [org.corfield.build :as bb]
            [build              :as b :refer [set-opts uber-file exec]]))

(defn source
  "Run the bot from source with the given config file."
  [opts]
  (if-let [config-file (:config-file opts)]
    (-> opts
        (set-opts)
        (assoc :main-opts ["-c" (str config-file)])
        (bb/run-task [:main]))
    (throw (ex-info ":config-file missing from tool invocation" (into {} opts)))))

(defn uber
  "Build the bot's uberjar, and run it with the given config file."
  [opts]
  (if-let [config-file (:config-file opts)]
    (do
      (b/uber opts)
      (exec ["java" "-XX:NativeMemoryTracking=summary" "-Xmx300m" "-Dclojure.server.repl={:port 5555 :accept clojure.core.server/repl}" "-jar" uber-file "-c" (str config-file)]))
    (throw (ex-info ":config-file missing from tool invocation" (into {} opts)))))
