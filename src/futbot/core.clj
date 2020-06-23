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

(ns futbot.core
  (:require [mount.core           :as mnt :refer [defstate]]
            [futbot.config        :as cfg]
            [java-time            :as tm]
            [chime.core           :as chime]
            [futbot.football-data :as fd]
            [futbot.template      :as tem]))

(defstate football-data-api-token
          :start (:football-data-api-token cfg/config))

(def tomorrow-at-midnight-UTC  (tm/with-clock (tm/system-clock "UTC") (tm/truncate-to (tm/plus (tm/zoned-date-time) (tm/days 1)) :days)))
(def every-day-at-midnight-UTC (chime/periodic-seq (tm/instant tomorrow-at-midnight-UTC)
                                                   (tm/period 1 :days)))

;                                                   (-> (java.time.LocalTime/of 0 0 0)
;                                                          (.adjustInto (java.time.ZonedDateTime/now (java.time.ZoneId/of "Etc/UTC")))
;                                                          .toInstant)
;                                                   (java.time.Period/ofDays 1)))

(defn daily-schedule
  []
  (try
    (let [todays-matches     (fd/matches-on-day football-data-api-token)
          daily-schedule-msg (tem/render "daily-schedule.ftl"
                                         {:day     (tm/format "yyyy-MM-dd" (tm/with-clock (tm/system-clock "UTC") (tm/zoned-date-time)))
                                          :matches todays-matches
                                          })]
      ;####TEST!!!!
      (println daily-schedule-msg))))


(defstate daily-schedule-job
          :start (chime/chime-at every-day-at-midnight-UTC
                                 daily-schedule)
          :stop (.close ^java.lang.AutoCloseable daily-schedule-job))
