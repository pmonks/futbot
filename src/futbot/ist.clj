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

(ns futbot.ist
  (:require [clojure.string     :as s]
            [clojure.java.io    :as io]
            [clojure.edn        :as edn]
            [markov-chains.core :as mc]
            [futbot.util        :as u]))

(def markov-chain (if-let [ist-markov-chain (io/resource "ist-markov-chain.edn")]
                    (edn/read-string (slurp ist-markov-chain))
                    (throw (RuntimeException. "ist-markov-chain classpath resource not found - did you remember to run the 'gen-ist-markov' alias first?"))))

(defn gen-title
  ([] (gen-title markov-chain))
  ([chain]
   (u/replace-all (s/join " "
                          (take 100   ; Make sure we eventually drop out
                                (take-while (partial not= "🔚")
                                            (drop-while #(or (= "🔚" %) (re-matches #"(\p{Punct})+" %))   ; Drop leading title breaks and punctuation
                                                        (mc/generate chain)))))
                  [[#"\s+([!?:;,\"…\*\.])" "$1"]])))  ; Collapse whitespace before punctuation
