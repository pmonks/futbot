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
  (:require [clojure.string        :as s]
            [clojure.java.io       :as io]
            [clojure.tools.logging :as log]
            [mount.core            :as mnt :refer [defstate]]
            [java-time             :as tm]
            [chime.core            :as chime]
            [discljord.events      :as de]
            [futbot.config         :as cfg]
            [futbot.football-data  :as fd]
            [futbot.jobs           :as job]
            [futbot.chat           :as chat]))

(defstate gc-job
          :start (let [seven-past-the-hour-UTC               (tm/with-clock (tm/system-clock "UTC") (tm/plus (tm/truncate-to (tm/zoned-date-time) :hours) (tm/minutes 7)))
                       every-hour-at-seven-past-the-hour-UTC (chime/periodic-seq (tm/instant seven-past-the-hour-UTC)
                                                                                 (tm/duration 1 :hours))]
                   (chime/chime-at every-hour-at-seven-past-the-hour-UTC
                                   (fn [_] (System/gc))))
          :stop (.close ^java.lang.AutoCloseable gc-job))

(defstate daily-job
          :start (let [tomorrow-at-midnight-UTC  (tm/with-clock (tm/system-clock "UTC") (tm/truncate-to (tm/plus (tm/zoned-date-time) (tm/days 1)) :days))
                       every-day-at-midnight-UTC (chime/periodic-seq (tm/instant tomorrow-at-midnight-UTC)
                                                                     (tm/period 1 :days))]
                   (chime/chime-at every-day-at-midnight-UTC
                                   (fn [_]
                                     (try
                                       (log/info "Daily job started...")
                                       (let [today                    (tm/with-clock (tm/system-clock "UTC") (tm/zoned-date-time))
                                             todays-scheduled-matches (fd/scheduled-matches-on-day cfg/football-data-api-token today)]
                                         (job/post-daily-schedule-to-channel! cfg/discord-message-channel
                                                                              cfg/daily-schedule-discord-channel-id
                                                                              (partial fd/match cfg/football-data-api-token)
                                                                              today
                                                                              todays-scheduled-matches)
                                         (job/schedule-todays-reminders! cfg/football-data-api-token
                                                                         cfg/discord-message-channel
                                                                         cfg/match-reminder-duration
                                                                         cfg/muted-leagues
                                                                         #(get cfg/country-to-channel % cfg/default-reminder-channel-id)
                                                                         cfg/referee-emoji
                                                                         today
                                                                         todays-scheduled-matches))
                                       (catch Exception e
                                         (log/error e "Unexpected exception while generating daily schedule"))
                                       (finally
                                         (log/info "Daily job finished"))))))
          :stop (.close ^java.lang.AutoCloseable daily-job))

; Bot functionality
(defn start-bot!
  "Starts the bot."
  []
  (job/schedule-todays-reminders! cfg/football-data-api-token
                                  cfg/discord-message-channel
                                  cfg/match-reminder-duration
                                  cfg/muted-leagues
                                  #(get cfg/country-to-channel % cfg/default-reminder-channel-id)
                                  cfg/referee-emoji)
  (log/info "futbot started")
  (de/message-pump! cfg/discord-event-channel chat/handle-discord-event))   ; Note: blocking fn
