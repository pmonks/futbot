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
            [cheshire.core         :as ch]))

(def api-host "https://api.football-data.org")

(def endpoint-matches-on-date "/v2/matches/?dateFrom=%s&dateTo=%s")
(def endpoint-match-details   "/v2/matches/%d")

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

(defn football-data-api-call
  "Calls the given football-data API endpoint using the given token, parses the result, and returns it.  Throws an ex-info if an error occurs (map contains HTTP :status)."
  [endpoint token]
  (let [api-url                     (str api-host endpoint)
        options                     {:headers {"X-Auth-Token" token}}
        {:keys [status error body]} @(http/get api-url options)]
    (if error
      (throw (ex-info  (format "football-data API call (%s) failed" api-url) {:status status} error))
      (ch/parse-string body
                       clojurise-json-key))))

(defn matches-on-day
  "Returns a list of all scheduled matches visible to the given token, on the given day (defaults to today UTC if not otherwise specified), sorted by scheduled time and then by competition."
  ([token] (matches-on-day token (tm/with-clock (tm/system-clock "UTC") (tm/zoned-date-time))))
  ([token day]
    (let [day-as-string (tm/format "yyyy-MM-dd" day)
          api-call      (format endpoint-matches-on-date day-as-string day-as-string)]
      (sort-by #(str (:utc-date %) "-" (:name (:competition %)))
               (filter #(= (:status %) "SCHEDULED") (:matches (football-data-api-call api-call token)))))))

(defn match
  "Returns match details for a single match."
  [token match-id]
  (football-data-api-call (format endpoint-match-details match-id) token))
