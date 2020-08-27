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

(ns futbot.jobs
  (:require [clojure.string        :as s]
            [clojure.java.io       :as io]
            [clojure.tools.logging :as log]
            [java-time             :as tm]
            [chime.core            :as chime]
            [discljord.messaging   :as dm]
            [futbot.football-data  :as fd]
            [futbot.pdf            :as pdf]
            [futbot.flags          :as fl]))

(defn post-daily-schedule-to-channel!
  "Generates and posts the daily-schedule (as an attachment) to the Discord channel identified by channel-id."
  [discord-message-channel
   channel-id
   match-details-fn
   today
   todays-matches]
  (System/gc)   ; Dear Mx JVM, now would be a *great* time to garbage collect...
  (let [today-str (tm/format "yyyy-MM-dd" today)]
    (if (seq todays-matches)
      (let [pdf-file    (pdf/generate-daily-schedule match-details-fn today todays-matches)
            pdf-file-is (io/input-stream pdf-file)]
        (dm/create-message! discord-message-channel
                            channel-id
                            :content (str "Here are the scheduled matches for " today-str ":")
                            :stream {:content pdf-file-is :filename (str "daily-schedule-" today-str ".pdf")}))
      (dm/create-message! discord-message-channel
                          channel-id
                          :content (str "There are no matches scheduled for today (" today-str ").")))))

(defn- referee-name
  [referee-emoji
   referee]
  (let [name (:name referee)]
    (if (s/blank? name)
      "[unnamed referee]"
      (if-let [emoji (get referee-emoji name)]
        (str name " " emoji)
        name))))

(defn- referee-names
  [referee-emoji
   referees]
  (if (seq (remove s/blank? (map :name referees)))   ; Make sure we have at least one named referee
    (s/join ", " (map (partial referee-name referee-emoji) referees))  ; And if so, include "unnamed" referees in the result, since position matters (CR, AR1, AR2, 4TH, VAR, etc.)
    "¯\\_(ツ)_/¯"))

(defn post-match-reminder-to-channel!
  [football-data-api-token
   discord-message-channel
   match-reminder-duration
   country-to-channel-fn
   referee-emoji
   match-id & _]
  (try
    (log/info (str "Sending reminder for match " match-id "..."))
    (if-let [{head-to-head :head2head
              match        :match}    (fd/match football-data-api-token match-id)]
      (let [league        (s/trim (get-in match [:competition :name]))
            country       (s/trim (get-in match [:competition :area :code]))
            channel-id    (country-to-channel-fn country)
            starts-in-min (try
                            (.toMinutes (tm/duration (tm/zoned-date-time)
                                                     (tm/with-clock (tm/system-clock "UTC") (tm/zoned-date-time (:utc-date match)))))
                            (catch Exception e
                              (.toMinutes ^java.time.Duration match-reminder-duration)))
            flag          (if-let [flag (fl/emoji (get-in match [:competition :area :code]))]
                            flag
                            "🏴‍☠️")
            match-prefix  (str flag " " league ": **" (get-in match [:home-team :name] "Unknown") " vs " (get-in match [:away-team :name] "Unknown") "**")
            message       (case (:status match)
                            "SCHEDULED" (str match-prefix " starts in " starts-in-min " minutes.\nReferees: " (referee-names referee-emoji (:referees match)))
                            "POSTPONED" (str match-prefix ", which was due to start in " starts-in-min " minutes, has been postponed.")
                            "CANCELED"  (str match-prefix ", which was due to start in " starts-in-min " minutes, has been canceled.")
                            nil)]
        (if message
          (dm/create-message! discord-message-channel
                              channel-id
                              :content message)
          (log/warn (str "Match " match-id " had an unexpected status: " (:status match) ". No reminder message sent."))))
      (log/warn (str "Match " match-id " was not found by football-data.org. No reminder message sent.")))
    (catch Exception e
      (log/error e "Unexpected exception while sending reminder for match " match-id))
    (finally
      (log/info (str "Finished sending reminder for match " match-id)))))

(defn schedule-match-reminder!
  "Schedules a reminder for the given match."
  [football-data-api-token
   discord-message-channel
   match-reminder-duration
   muted-leagues
   country-to-channel-fn
   referee-emoji
   match]
  (let [now           (tm/with-clock (tm/system-clock "UTC") (tm/zoned-date-time))
        match-time    (tm/zoned-date-time (:utc-date match))
        match-league  (get-in match [:competition :name])
        reminder-time (tm/minus match-time (tm/plus match-reminder-duration (tm/seconds 10)))]
    (if-not (some (partial = match-league) muted-leagues)
      (if (tm/before? now reminder-time)
        (do
          (log/info (str "Scheduling reminder for match " (:id match) " at " reminder-time))
          (chime/chime-at [reminder-time]
                          (partial post-match-reminder-to-channel!
                                   football-data-api-token
                                   discord-message-channel
                                   match-reminder-duration
                                   country-to-channel-fn
                                   referee-emoji
                                   (:id match))))
        (log/info (str "Reminder time " reminder-time " for match " (:id match) " has already passed - not scheduling a reminder.")))
      (log/info (str "Match " (:id match) " is in a muted league - not scheduling a reminder.")))))

(defn schedule-todays-reminders!
  "Schedules reminders for the remainder of today's matches."
  ([football-data-api-token
    discord-message-channel
    match-reminder-duration
    muted-leagues
    country-to-channel-fn
    referee-emoji]
    (let [today                    (tm/with-clock (tm/system-clock "UTC") (tm/zoned-date-time))
          todays-scheduled-matches (fd/scheduled-matches-on-day football-data-api-token today)]
      (schedule-todays-reminders! football-data-api-token
                                  discord-message-channel
                                  match-reminder-duration
                                  muted-leagues
                                  country-to-channel-fn
                                  referee-emoji
                                  today
                                  todays-scheduled-matches)))
  ([football-data-api-token
    discord-message-channel
    match-reminder-duration
    muted-leagues
    country-to-channel-fn
    referee-emoji
    today
    todays-scheduled-matches]
    (if (seq todays-scheduled-matches)
      (doall (map (partial schedule-match-reminder! football-data-api-token
                                                    discord-message-channel
                                                    match-reminder-duration
                                                    muted-leagues
                                                    country-to-channel-fn
                                                    referee-emoji)
                  (distinct todays-scheduled-matches)))
      (log/info "No matches remaining today - not scheduling any reminders."))))
