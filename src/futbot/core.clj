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

(defstate match-reminder-duration-mins
          :start (if-let [duration (:match-reminder-duration-mins cfg/config)]
                   duration
                   15))    ; Default to 15 minutes

(defstate match-reminder-duration
          :start (tm/minutes match-reminder-duration-mins))

(defstate league-to-channel
          :start (:league-to-channel-map cfg/config))

(defstate default-league-channel-id
          :start (let [channel-id (:default-league-channel-id cfg/config)]
                   (if-not (s/blank? channel-id)
                     channel-id
                     (throw (ex-info "Default league Discord channel id not provided" {})))))


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
    (log/info (str "Sending reminder for match " match-id "..."))
    (if-let [{head-to-head :head2head
              match        :match}    (fd/match football-data-api-token match-id)]
      (let [channel-id    (get league-to-channel (get-in match [:competition :name]) default-league-channel-id)
            starts-in-min (try
                            (.toMinutes (tm/duration (tm/zoned-date-time)
                                                     (tm/with-clock (tm/system-clock "UTC") (tm/zoned-date-time (:utc-date match)))))
                            (catch Exception e
                              match-reminder-duration-mins))
            opponents     (str (get-in match [:home-team :name] "Unknown") " vs " (get-in match [:away-team :name] "Unknown"))
            message       (case (:status match)
                            "SCHEDULED" (str "⚽️ **" opponents " starts in " starts-in-min " minutes.**"
                                             (if-let [referees (seq (:referees match))]
                                               (str "\nReferees: " (s/join ", " (map :name referees)))))
                            "POSTPONED" (str "⚽️ **" opponents ", due to start in " starts-in-min " minutes, has been postponed.**")
                            "CANCELED"  (str "⚽️ **" opponents ", due to start in " starts-in-min " minutes, has been canceled.**")
                            nil)]
        (if message
          (dm/create-message! discord-message-channel
                               channel-id
                               :content message)
          (log/info (str "Match " match-id " had an unexpected status: " (:status match) ". No reminder message sent."))))
      (log/warn (str "Match " match-id " was not found by football-data.org. No reminder message sent.")))
    (catch Exception e
      (log/error e "Unexpected exception while sending reminder for match " match-id))
    (finally
      (log/info (str "Finished sending reminder for match " match-id)))))

(defn schedule-match-reminder!
  "Schedules a reminder for the given match."
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
    (let [today                    (tm/with-clock (tm/system-clock "UTC") (tm/zoned-date-time))
          todays-scheduled-matches (fd/scheduled-matches-on-day football-data-api-token today)]
      (post-daily-schedule-to-channel! channel-id today todays-scheduled-matches)
      (doall (map schedule-match-reminder! todays-scheduled-matches)))
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

(defn schedule-todays-reminders!
  "Schedules reminders for the remainder of today's matches."
  []
  (log/debug "Scheduling reminders for the remainder of today's matches...")
  (let [today                    (tm/with-clock (tm/system-clock "UTC") (tm/zoned-date-time))
        todays-scheduled-matches (fd/scheduled-matches-on-day football-data-api-token today)]
    (if (seq todays-scheduled-matches)
      (doall (map schedule-match-reminder! todays-scheduled-matches))
      (log/debug "No remaining matches scheduled for today"))))

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



; Bot functionality
(defn start-bot!
  "Starts the bot."
  []
  (schedule-todays-reminders!)
  (log/info "futbot started")
  (de/message-pump! discord-event-channel handle-discord-event))   ; Note: blocking fn
