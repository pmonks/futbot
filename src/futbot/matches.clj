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

(ns futbot.matches
  (:require [clojure.string              :as s]
            [clojure.tools.logging       :as log]
            [java-time                   :as tm]
            [chime.core                  :as chime]
            [futbot.util                 :as u]
            [futbot.discord.message-util :as mu]
            [futbot.source.football-data :as fd]
            [futbot.leagues              :as lg]
            [futbot.flags                :as fl]))

(def ^:private default-match-duration (+ 45 15 45))

; This map is keyed by the football-data.org role literals, with values being a tuple of [display-order display-abbreviation].
(def ^:private role-map {
  "REFEREE"                    [1 "CR"]
  "ASSISTANT_REFEREE_N1"       [2 "AR1"]
  "ASSISTANT_REFEREE_N2"       [3 "AR2"]
  "FOURTH_OFFICIAL"            [4 "4O"]
  "VIDEO_ASSISANT_REFEREE_N1"  [5 "VAR1"]    ; Typo in football-data.org (as of 2021-04-05)
  "VIDEO_ASSISTANT_REFEREE_N1" [5 "VAR1"]    ; Fixed as of 2022-03-28
  "VIDEO_ASSISANT_REFEREE_N2"  [6 "VAR2"]    ; Typo in football-data.org (as of 2021-04-05)
  "VIDEO_ASSISTANT_REFEREE_N2" [6 "VAR2"]    ; Fixed as of 2022-03-28
  "VIDEO_ASSISTANT_REFEREE_N3" [7 "VAR3"]})

(def ^:private shrug-text   "¯\\_(ツ)_/¯")
(def ^:private shrug-reacts ["<:shrug_east:802047895304929310>"
                             "<:shrug_cakir:859116470343434300>"
                             "<:shrug_dean:859125521018519613>"
                             "<:shrug_shemesh:859135806014488626>"
                             "<:shrug_penso:859140464749182997>"
                             "<:shrug_kuipers:861638186480041984>"])

(defn- referee-name
  [referee]
  (let [name         (if-let [name (:name referee)]
                       name
                       (rand-nth shrug-reacts))
        country-flag (fl/emoji-from-name (:nationality referee))
        role         (second (get role-map (:role referee)))]
    (str (when role         (str "**" role ":** "))
         name
         (when country-flag (str " " country-flag)))))

(defn- referee-sort-by
  [referee]
  (let [role-order (first (get role-map (:role referee)))
        name       (:name referee)]
    (str role-order name)))

(defn- referee-names
  [referees]
  (if (seq (remove s/blank? (map :name referees)))   ; Make sure we have at least one named referee
    (s/join "\n" (map referee-name (sort-by referee-sort-by referees)))  ; And if so, include "unnamed" referees in the result, since role matters (CR, AR1, AR2, 4TH, VAR, etc.)
    (rand-nth shrug-reacts)))

(def ^:private match-status-to-emoji {
  "SCHEDULED" "⏰"
  "POSTPONED" "🔜"
  "CANCELED"  "❌"
  "SUSPENDED" "⚡️"
  "IN_PLAY"   "⚽️"
  "PAUSED"    "⏸"
  "FINISHED"  "🏁"
  "AWARDED"   "🏆"
  })

(defn- match-channel-link
  [{:keys [country-to-channel default-reminder-channel-id]} match]
  (let [country            (s/trim (get-in match [:competition :area :code]))
        country-channel-id (u/getrn country-to-channel country default-reminder-channel-id)]
    (mu/channel-link country-channel-id)))

(defn- match-embed-template
  [match]
  (let [league          (s/trim (get-in match [:competition :name]))
        country         (s/trim (get-in match [:competition :area :code]))
        league-logo-url (if-let [league-logo-url (lg/league-to-logo-url league)]
                          league-logo-url
                          (fl/flag-url country))]
    (assoc (mu/embed-template)
           :footer {:text     (str (s/upper-case (s/trim (get-in match [:competition :area :name]))) ": " league)   ; This format matches what ToonBot uses
                    :icon_url league-logo-url})))

(def ^:private card-type-to-emoji {    ; Sadly we can't use our nice custom emoji here, as they end up in a code block and Discord doesn't support custom emoji in code blocks...
  "YELLOW_CARD"     "🟨"
  "YELLOW_RED_CARD" "🟨🟥"
  "RED_CARD"        "🟥"
  "CANCELLED_CARD"  "⬜️❌"
  })

(defn- match-event-row
  [match-event]
  (when match-event
    (format "%-4.4s %-4.4s %-19.19s %-19.19s"  ; This makes the absolute most of the available embed real estate. Note that truncation lengths below must match.
            (str (:minute match-event) "'")
            (if (:card match-event) (get card-type-to-emoji (:card match-event) "❔") "⚽️")
            (u/truncate (if (:card match-event) (get-in match-event [:player :name] shrug-text) (get-in match-event [:scorer :name] shrug-text)) 19)
            (u/truncate (get-in match-event [:team :name] shrug-text) 19))))

(defn- match-events-table
  [match]
  (when match
    (if-let [events (seq (sort-by :minute (concat (get-in match [:goals]) (get-in match [:bookings]))))]
      (str "```"
           "When What Who                 Team\n"
           "---- ---- ------------------- -------------------\n"
           (s/join "\n" (keep identity (map match-event-row events)))
           (when (= "PENALTY_SHOOTOUT" (get-in match [:score :duration]))
             (str "\n\nKFTM: "
                  (get-in match [:home-team :name] shrug-text) " " (get-in match [:score :penalties :home-team] "❔") ", "
                  (get-in match [:away-team :name] shrug-text) " " (get-in match [:score :penalties :away-team] "❔")))
           "\n```")
      "```No events in this match. 🥱```")))

(defn ^:private estimated-minutes-left-in-match
  "Estimates how many minutes are left in the match, or nil if no such estimate can be made (e.g. when the match is not currently being played)."
  [match]
  (case (:status match)
    "IN_PLAY" (when-let [match-minute (:minute match)] (max 0 (- 90 (u/parse-int match-minute))))
    nil))

(defn post-match-summary-to-channel!
  [{:keys [football-data-api-token discord-message-channel match-reminder-channel-id] :as config}
   match-id
   retry-number
   & _]
  (try
    (if-let [{match :match} (fd/match football-data-api-token match-id)]
      (let [status (:status match)]
        (cond
          ; Match is not yet over, check again later
          (or (= status "SCHEDULED")
              (= status "IN_PLAY")
              (= status "PAUSED"))  (let [minutes-left (estimated-minutes-left-in-match match)
                                          retry-after  (if ((fnil >= 0) minutes-left 1)                       ; If the estimated minutes left >= 1...
                                                         minutes-left                                         ; ...retry then
                                                         (min 60 (max 1 (u/nth-fibonacci retry-number))))]    ; ...else, retry in between 1 minute and 1 hour, with Fibonacci backoff in between
                                      (log/info (str "Match " match-id " has not yet finished (attempt " (inc retry-number) "); retrying in " (str retry-after) " minute(s)."))
                                      (chime/chime-at [(tm/plus (tm/instant) (tm/minutes retry-after))] (partial post-match-summary-to-channel! config match-id (inc retry-number))))
          ; Match is over; send summary message
          (or (= status "FINISHED")
              (= status "AWARDED")) (let [match-summary (str (get match-status-to-emoji (:status match) "❔")
                                                             "  **" (get-in match [:home-team :name] "Unknown") "** vs **" (get-in match [:away-team :name] "Unknown") "**, "
                                                             "final score: **" (get-in match [:score :full-time :home-team] (str (rand-nth shrug-reacts) " "))
                                                                               "-"
                                                                               (get-in match [:score :full-time :away-team] (str " " (rand-nth shrug-reacts)))
                                                                               "**")
                                          description   (str match-summary "\n"
                                                             (match-events-table match) "\n"
                                                             "Discuss in " (match-channel-link config match) ".")
                                          embed         (assoc (match-embed-template match)
                                                               :description description)]
                                      (log/info (str "Sending summary for match " match-id "..."))
                                      (mu/create-message! discord-message-channel
                                                          match-reminder-channel-id
                                                          :embed embed)
                                      (log/info (str "Finished sending summary for match " match-id)))
          :else (log/warn (str "Unexpected status (" status ") for match " match-id "."))))
      (log/warn (str "Match " match-id " was not found by football-data.org. No summary message sent.")))
    (catch Exception e
      (u/log-exception e (str "Unexpected exception while sending summary for match " match-id)))))

(defn schedule-in-progress-match-summary!
  [{:keys [muted-leagues] :as config}
   match]
  (let [match-id     (:id match)
        match-league (get-in match [:competition :name])]
    (if-not (some (partial = match-league) muted-leagues)
      (let [post-match-summary-estimated-time (tm/plus (tm/instant) (tm/minutes (if-let [minutes-left (estimated-minutes-left-in-match match)]
                                                                                  minutes-left
                                                                                  1)))]
        (log/info (str "Scheduling summary for in-progress match " match-id "; first run will be at " post-match-summary-estimated-time))
        (chime/chime-at [post-match-summary-estimated-time] (partial post-match-summary-to-channel! config match-id 0)))
      (log/info (str "In-progress match " match-id " is in a muted league - not scheduling a summary.")))))

(defn schedule-in-progress-match-summaries!
  "Schedules match summary jobs for all matches that are in progress at the time it's called."
  [{:keys [football-data-api-token] :as config}]
  (if-let [in-progress-matches (fd/matches-in-play football-data-api-token)]
    (doall (map (partial schedule-in-progress-match-summary! config) in-progress-matches))
    (log/info "No matches currently in progress; not scheduling any match summary jobs.")))

(defn post-match-reminder-to-channel!
  [{:keys [football-data-api-token discord-message-channel match-reminder-duration match-reminder-channel-id] :as config}
   match-id & _]
  (try
    (log/info (str "Sending reminder for match " match-id "..."))
    (if-let [{match :match} (fd/match football-data-api-token match-id)]
      (let [starts-at          (tm/instant (:utc-date match))
            match-summary      (str (get match-status-to-emoji (:status match) "❔")
                                    "  **" (get-in match [:home-team :name] "Unknown") "** vs **" (get-in match [:away-team :name] "Unknown") "**")
            match-channel-link (match-channel-link config match)
            description        (case (:status match)
                                 "SCHEDULED" (str match-summary " kick off " (mu/timestamp-tag starts-at \R) " (at " (mu/timestamp-tag starts-at \t) " local time).\n\n"
                                                  "[Find out how to watch here](https://www.livesoccertv.com/), and discuss in " match-channel-link ".")
                                 "IN_PLAY"   (str match-summary ", which was originally due to start at " (mu/timestamp-tag starts-at \t) ", started early and is in progress.\n\n"
                                                  "[Find out how to watch here](https://www.livesoccertv.com/), and discuss in " match-channel-link ".")
                                 "FINISHED"  (str match-summary " has finished.\n\n"
                                                  "Discuss in " match-channel-link ".")
                                 (str match-summary ", which was due to start " (mu/timestamp-tag starts-at \R) " (at " (mu/timestamp-tag starts-at \t) "), has been " (s/lower-case (:status match)) ".\n"\n)
                                      "Discuss in " match-channel-link ".")
            embed              (assoc (match-embed-template match)
                                      :description description
                                      :fields [
                                        {:name "Referees" :value (referee-names (:referees match))}
                                      ]
                                     )]
        (mu/create-message! discord-message-channel
                            match-reminder-channel-id
                            :embed embed)
        (let [post-match-summary-estimated-time (tm/plus (tm/instant) match-reminder-duration (tm/minutes default-match-duration))]
          (log/info (str "Scheduling summary for in-progress match " match-id "; first run will be at " post-match-summary-estimated-time))
          (chime/chime-at [post-match-summary-estimated-time] (partial post-match-summary-to-channel! config match-id 0))))
      (log/warn (str "Match " match-id " was not found by football-data.org. No reminder message sent.")))
    (log/info (str "Finished sending reminder for match " match-id))
    (catch Exception e
      (u/log-exception e (str "Unexpected exception while sending reminder for match " match-id)))))

(defn schedule-match-reminder!
  "Schedules a reminder for the given match."
  [{:keys [match-reminder-duration muted-leagues] :as config}
   match]
  (let [now           (u/in-tz "UTC" (tm/zoned-date-time))
        match-time    (tm/zoned-date-time (:utc-date match))
        match-league  (get-in match [:competition :name])
        reminder-time (tm/minus match-time (tm/plus match-reminder-duration (tm/seconds 10)))]
    (if-not (some (partial = match-league) muted-leagues)
      (if (tm/before? now reminder-time)
        (do
          (log/info (str "Scheduling reminder for match " (:id match) " at " reminder-time))
          (chime/chime-at [reminder-time] (partial post-match-reminder-to-channel! config (:id match))))
        (log/info (str "Reminder time " reminder-time " for match " (:id match) " has already passed - not scheduling a reminder.")))
      (log/info (str "Match " (:id match) " is in a muted league - not scheduling a reminder.")))))

(defn schedule-todays-match-reminders!
  "Schedules reminders for the remainder of today's matches."
  [{:keys [football-data-api-token] :as config}]
   (let [today                    (u/in-tz "UTC" (tm/zoned-date-time))
         todays-scheduled-matches (fd/scheduled-matches-on-day football-data-api-token today)]
     (if (seq todays-scheduled-matches)
       (doall (map (partial schedule-match-reminder! config)
                   (distinct todays-scheduled-matches)))
       (log/info "No matches remaining today - not scheduling any reminders."))))
