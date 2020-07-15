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

(ns futbot.football-data
  (:require [clojure.string        :as s]
            [clojure.tools.logging :as log]
            [java-time             :as tm]
            [org.httpkit.client    :as http]
            [cheshire.core         :as ch]
            [futbot.util           :as u]))

(def api-host "https://api.football-data.org")

(def endpoint-matches-on-date "/v2/matches/?dateFrom=%s&dateTo=%s")
(def endpoint-match-details   "/v2/matches/%d")

(def maximum-retries 3)

(defn football-data-api-call
  "Calls the given football-data API endpoint using the given token, parses the result and returns it.  Throws an ex-info if an error occurs (the ex-info map contains HTTP :status)."
  [token endpoint]
  (let [api-url (str api-host endpoint)
        options {:headers {"X-Auth-Token" token}}]
    (loop [attempt 1]
      (log/debug "Calling" api-url)
      (let [{:keys [status headers body error]} @(http/get api-url options)]
        (if (= status 429)                   ; We got throttled
          (if (> attempt maximum-retries)    ; Bail out after too many retry attempts
            (throw (ex-info (format "Too many retries (%d) to football-data API %s" attempt api-url) {:status status} error))
            (let [counter-reset-ms (* 1000 (u/parse-int (s/trim (get headers "X-RequestCounter-Reset"))))  ; Find out how long football-data wants us to wait
                  retry-in-ms      (+ counter-reset-ms (rand-int (* 1000 attempt)))]            ; Add some randomness to try to avoid stampeding herds
              (log/warn (format "football-data API call attempt %d throttled, waiting %dms before retrying." attempt retry-in-ms))
              (Thread/sleep retry-in-ms)
              (recur (inc attempt))))
          (if error
            (throw (ex-info (format "football-data API call (%s) failed" api-url) {:status status} error))
            (ch/parse-string body
                             u/clojurise-json-key)))))))

(defn matches-on-day
  "Returns a list of all matches visible to the given token, on the given day (defaults to today UTC if not otherwise specified), sorted by scheduled time and then by competition."
  ([token] (matches-on-day token (tm/with-clock (tm/system-clock "UTC") (tm/zoned-date-time))))
  ([token day]
    (let [day-as-string (tm/format "yyyy-MM-dd" day)
          api-call      (format endpoint-matches-on-date day-as-string day-as-string)]
      (sort-by #(str (:utc-date %) "-" (:name (:competition %)))
               (:matches (football-data-api-call token api-call))))))

(defn scheduled-matches-on-day
  "Returns a list of all scheduled matches visible to the given token, on the given day (defaults to today UTC if not otherwise specified), sorted by scheduled time and then by competition."
  ([token]     (filter #(= (:status %) "SCHEDULED") (matches-on-day token)))
  ([token day] (filter #(= (:status %) "SCHEDULED") (matches-on-day token day))))

(defn match
  "Returns match details for a single match."
  [token match-id]
  (football-data-api-call token (format endpoint-match-details match-id)))
