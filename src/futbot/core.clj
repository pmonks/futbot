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
            [clojure.core.async    :as async]
            [clojure.tools.logging :as log]
            [futbot.config         :as cfg]
            [mount.core            :as mnt :refer [defstate]]
            [java-time             :as tm]
            [chime.core            :as chime]
            [discljord.connections :as dc]
            [discljord.messaging   :as dm]
            [discljord.events      :as de]
            [futbot.football-data  :as fd]
            [futbot.jobs           :as job]
            [futbot.chat           :as chat]))

(defstate football-data-api-token
          :start (let [token (:football-data-api-token cfg/config)]
                   (if-not (s/blank? token)
                     token
                     (throw (ex-info "football-data.org API token not provided" {})))))

(defstate discord-api-token
          :start (let [token (:discord-api-token cfg/config)]
                   (if-not (s/blank? token)
                     token
                     (throw (ex-info "Discord API token not provided" {})))))

(defstate discord-event-channel
          :start (async/chan (:discord-event-channel-size cfg/config))
          :stop  (async/close! discord-event-channel))

(defstate discord-connection-channel
          :start (if-let [connection (dc/connect-bot! discord-api-token discord-event-channel)]
                   connection
                   (throw (ex-info "Failed to connect bot to Discord" {})))
          :stop  (dc/disconnect-bot! discord-connection-channel))

(defstate discord-message-channel
          :start (if-let [connection (dm/start-connection! discord-api-token)]
                   connection
                   (throw (ex-info "Failed to connect to Discord message channel" {})))
          :stop  (dm/stop-connection! discord-message-channel))

(defstate daily-schedule-discord-channel-id
          :start (let [channel-id (:daily-schedule-discord-channel-id cfg/config)]
                   (if-not (s/blank? channel-id)
                     channel-id
                     (throw (ex-info "Daily schedule Discord channel id not provided" {})))))

(defstate match-reminder-duration
          :start (tm/minutes (if-let [duration (:match-reminder-duration-mins cfg/config)]
                               duration
                               15)))  ; Default is 15 minutes

(defstate league-to-channel
          :start (:league-to-channel-map cfg/config))

(defstate default-league-channel-id
          :start (let [channel-id (:default-league-channel-id cfg/config)]
                   (if-not (s/blank? channel-id)
                     channel-id
                     (throw (ex-info "Default league Discord channel id not provided" {})))))

(defstate muted-leagues
          :start (:muted-leagues cfg/config))

(defstate daily-job
          :start (let [tomorrow-at-midnight-UTC  (tm/with-clock (tm/system-clock "UTC") (tm/truncate-to (tm/plus (tm/zoned-date-time) (tm/days 1)) :days))
                       every-day-at-midnight-UTC (chime/periodic-seq (tm/instant tomorrow-at-midnight-UTC)
                                                                     (tm/period 1 :days))]
                   (chime/chime-at every-day-at-midnight-UTC
                                   (fn [_]
                                     (try
                                       (log/info "Daily job started...")
                                       (let [today                    (tm/with-clock (tm/system-clock "UTC") (tm/zoned-date-time))
                                             todays-scheduled-matches (fd/scheduled-matches-on-day football-data-api-token today)]
                                         (job/post-daily-schedule-to-channel! discord-message-channel
                                                                              daily-schedule-discord-channel-id
                                                                              today
                                                                              todays-scheduled-matches)
                                         (job/schedule-todays-reminders! football-data-api-token
                                                                         discord-message-channel
                                                                         match-reminder-duration
                                                                         muted-leagues
                                                                         #(get-in league-to-channel % default-league-channel-id)
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
  (job/schedule-todays-reminders! football-data-api-token
                                  discord-message-channel
                                  match-reminder-duration
                                  muted-leagues
                                  #(get league-to-channel % default-league-channel-id))
  (log/info "futbot started")
  (de/message-pump! discord-event-channel chat/handle-discord-event))   ; Note: blocking fn
