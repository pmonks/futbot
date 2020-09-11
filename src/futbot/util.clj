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

(ns futbot.util
  (:require [clojure.string :as s]))

(defn parse-int
  [s]
  (try
    (Integer/parseInt s)
    (catch NumberFormatException nfe
      nil)))

(defn clojurise-json-key
  "Converts JSON string keys (e.g. \"fullName\") to Clojure keyword keys (e.g. :full-name)."
  [k]
  (keyword
    (s/replace
      (s/join "-"
              (map s/lower-case
                   (s/split k #"(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")))
      "_"
      "-")))

(defn replace-all
  "Takes a sequence of replacements, and applies all of them to the given string, in the order provided.  Each replacement in the sequence is a pair of values to be passed to clojure.string/replace (the 2nd and 3rd arguments)."
  [string replacements]
  (loop [s string
         f (first replacements)
         r (rest  replacements)]
    (if f
      (recur (s/replace s (first f) (second f))
             (first r)
             (rest  r))
      s)))

(defn human-readable-date-diff
  "Returns a string containing the human readable difference between two instants."
  [^java.time.Instant i1
   ^java.time.Instant i2]
  (format "%dd %dh %dm %d.%03ds" (.until i1 i2 (java.time.temporal.ChronoUnit/DAYS))
                                 (mod (.until i1 i2 (java.time.temporal.ChronoUnit/HOURS))     24)
                                 (mod (.until i1 i2 (java.time.temporal.ChronoUnit/MINUTES))   60)
                                 (mod (.until i1 i2 (java.time.temporal.ChronoUnit/SECONDS))   60)
                                 (mod (.until i1 i2 (java.time.temporal.ChronoUnit/MILLIS))  1000)))

(defn exit
  "Exits the program after printing the given message, and returns the given status code."
  ([]            (exit 0 nil))
  ([status-code] (exit status-code nil))
  ([status-code message]
   (when message
     (if (= 0 status-code)
       (println message)
       (binding [*out* *err*]
         (println message))))
   (flush)
   (shutdown-agents)
   (System/exit status-code)))
