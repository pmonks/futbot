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
            [mount.core            :as mnt :refer [defstate]]
            [futbot.config         :as cfg]
            [java-time             :as tm]
            [chime.core            :as chime]
            [futbot.football-data  :as fd]
            [futbot.pdf            :as pdf]
            [discljord.connections :as dc]
            [discljord.messaging   :as dm]
            [discljord.events      :as de]))

(defstate football-data-api-token
          :start (:football-data-api-token cfg/config))

(defstate discord-api-token
          :start (:discord-api-token cfg/config))

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
          :start (:daily-schedule-discord-channel-id cfg/config))

(defstate match-reminder-duration-mins
          :start (:match-reminder-duration-mins cfg/config))
(defstate match-reminder-duration
          :start (tm/minutes match-reminder-duration-mins))

(defstate league-to-channel
          :start (:league-to-channel-map cfg/config))

(defstate default-league-channel
          :start (:default-league-channel cfg/config))


; Timed jobs and related fns
(defn post-daily-schedule-to-channel!
  "Generates and posts the daily-schedule (as an attachment) to the Discord channel identified by channel-id."
  [channel-id today todays-matches]
  (let [today-str (tm/format "yyyy-MM-dd" today)]
    (if (seq todays-matches)
      (let [pdf-file    (pdf/generate-daily-schedule today todays-matches)
            pdf-file-is (io/input-stream pdf-file)]
        (dm/create-message! discord-message-channel
                             channel-id
                             :content (str "Here are the scheduled matches for " today-str ":")
                             :stream {:content pdf-file-is :filename (str "daily-schedule-" today-str ".pdf")}))
      (dm/create-message! discord-message-channel
                           channel-id
                           :content (str "Sadly there are no ⚽️ matches scheduled for today (" today-str "). 😢")))))

(defn post-match-reminder-to-channel!
  [match-id & args]
  (try
    (log/info (str "Reminder job for match " match-id " started..."))
    (if-let [updated-match-info (fd/match football-data-api-token match-id)]
      (let [channel-id (get-in league-to-channel [:competition :name] default-league-channel)
            message    (str (get-in [:match :home-team :name] updated-match-info) " vs " (get-in [:match :away-team :name] updated-match-info)
                            " starts in " match-reminder-duration-mins " minutes."
                            (if-let [referees (seq (get-in [:match :referees] updated-match-info))]
                              (str "\nReferees: " (s/join ", " referees))))]
        (dm/create-message! discord-message-channel
                             channel-id
                             :content message))
      (log/warn (str "Match " match-id " was not found by football-data.org")))
    (catch Exception e
      (log/error e "Unexpected exception while sending reminder for match " match-id))
    (finally
      (log/info (str "Reminder job for match " match-id " finished")))))

(defn create-match-reminder-job!
  "Schedules a reminder job for the given match."
  [match]
  (let [now           (tm/with-clock (tm/system-clock "UTC") (tm/zoned-date-time))
        match-time    (tm/zoned-date-time (:utc-date match))
        reminder-time (tm/minus match-time match-reminder-duration)]
    (if (tm/before? now reminder-time)
      (do
        (log/debug (str "Scheduling reminder for match " (:id match) " at " reminder-time))
        (chime/chime-at [reminder-time]
                        (partial post-match-reminder-to-channel! (:id match))))
      (log/debug (str "Reminder time " reminder-time " for match " (:id match) " has already passed - not scheduling a reminder.")))))

(defn daily-job-fn!
  [channel-id & args]
  (try
    (log/info "Daily job started...")
    (let [today          (tm/with-clock (tm/system-clock "UTC") (tm/zoned-date-time))
          todays-matches (fd/matches-on-day football-data-api-token today)]
      (post-daily-schedule-to-channel! channel-id today todays-matches)
      (doall (map create-match-reminder-job! todays-matches)))
    (catch Exception e
      (log/error e "Unexpected exception while generating daily schedule"))
    (finally
      (log/info "Daily job finished"))))

(def tomorrow-at-midnight-UTC  (tm/with-clock (tm/system-clock "UTC") (tm/truncate-to (tm/plus (tm/zoned-date-time) (tm/days 1)) :days)))
(def every-day-at-midnight-UTC (chime/periodic-seq (tm/instant tomorrow-at-midnight-UTC)
                                                   (tm/period 1 :days)))

(defstate daily-job
          :start (chime/chime-at every-day-at-midnight-UTC
                                 (partial daily-job-fn! daily-schedule-discord-channel-id))
          :stop (.close ^java.lang.AutoCloseable daily-job))

(defn register-todays-reminders!
  "Registers reminders for the remainder of today's matches."
  []
  (let [today          (tm/with-clock (tm/system-clock "UTC") (tm/zoned-date-time))
        todays-matches (fd/matches-on-day football-data-api-token today)]
    (doall (map create-match-reminder-job! todays-matches))))

; For testing purposes
(comment
(def run-shortly (tm/with-clock (tm/system-clock "UTC") (tm/plus (tm/zoned-date-time) (tm/minutes 1))))
(defstate run-shortly-job
          :start (chime/chime-at [run-shortly]
                                 (partial post-daily-schedule! daily-schedule-discord-channel-id))
          :stop (.close ^java.lang.AutoCloseable run-shortly-job))
)



; Responsive fns
(defmulti handle-discord-event
  "Discord event handler"
  (fn [event-type event-data]
    event-type))

; Default Discord event handler (noop)
(defmethod handle-discord-event :default
  [event-type event-data])

;####TODO: Implement responsive messaging (i.e. chatops from humans to the bot)

(defn start-bot!
  "Starts the bot, which involves starting the message pump and creating timed jobs for all of today's match reminders."
  []
  (de/message-pump! discord-event-channel handle-discord-event)
  (register-todays-reminders!))
