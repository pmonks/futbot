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

(ns ist
  "IST script for futbot.

For more information, run:

clojure -A:deps -T:ist help/doc"
  (:require [org.corfield.build :as bb]
            [build              :as b]))

(defn generate-markov
  "(Re)generate the IST Markov chain."
  [opts]
  (when-not (:youtube-api-key opts)
    (throw (ex-info ":youtube-api-key missing from tool invocation" (into {} opts))))
  (-> opts
      (b/set-opts)
      (assoc :main-opts [(str (:youtube-api-key opts))])
      (bb/run-task [:gen-ist-markov])))
