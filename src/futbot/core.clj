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
  (:require [clojure.string                   :as s]
            [clojure.tools.logging            :as log]
            [java-time                        :as tm]
            [chime.core                       :as chime]
            [futbot.util                      :as u]
            [futbot.message-util              :as mu]
            [futbot.source.football-data      :as fd]
            [futbot.source.dutch-referee-blog :as drb]
            [futbot.source.cnra               :as cnra]
            [futbot.source.youtube            :as yt]
            [futbot.leagues                   :as lg]
            [futbot.flags                     :as fl]))

(def ^:private default-match-duration (+ 45 15 45))

; This map is keyed by the football-data.org role literals, with values being a tuple of [display-order display-abbreviation].
(def ^:private role-map {
  "REFEREE"                    [1 "CR"]
  "ASSISTANT_REFEREE_N1"       [2 "AR1"]
  "ASSISTANT_REFEREE_N2"       [3 "AR2"]
  "FOURTH_OFFICIAL"            [4 "4O"]
  "VIDEO_ASSISANT_REFEREE_N1"  [5 "VAR1"]    ; Typo in football-data.org (as of 2021-04-05)
  "VIDEO_ASSISTANT_REFEREE_N1" [5 "VAR1"]    ; Placeholder in case Daniel fixes it without telling us
  "VIDEO_ASSISANT_REFEREE_N2"  [6 "VAR2"]    ; Typo in football-data.org (as of 2021-04-05)
  "VIDEO_ASSISTANT_REFEREE_N2" [6 "VAR2"]})  ; Placeholder in case Daniel fixes it without telling us

;(def ^:private shrug "¯\\_(ツ)_/¯")
(def ^:private shrug "<:shrug:802047895304929310>")

(defn- referee-name
  [referee]
  (let [name         (if-let [name (:name referee)]
                       name
                       shrug)
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
    shrug))

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
  [country-to-channel-fn match]
  (let [country            (s/trim (get-in match [:competition :area :code]))
        country-channel-id (country-to-channel-fn country)]
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
  })

(defn- match-event-row
  [match-event]
  (when match-event
    (format "%-4.4s %-4.4s %-19.19s %-19.19s"  ; This makes the absolute most of the available embed real estate. Note that truncation lengths below must match.
            (str (:minute match-event) "'")
            (if (:card match-event) (get card-type-to-emoji (:card match-event) "❔") "⚽️")
            (u/truncate (if (:card match-event) (get-in match-event [:player :name]) (get-in match-event [:scorer :name])) 19)
            (u/truncate (get-in match-event [:team :name]) 19))))

(defn- match-events-table
  [match]
  (when match
    (if-let [events (seq (sort-by :minute (concat (get-in match [:goals]) (get-in match [:bookings]))))]
      (str "```"
           "When What Who                 Team\n"
           "---- ---- ------------------- -------------------\n"
           (s/join "\n" (keep identity (map match-event-row events)))
           "\n```")
      "```No events in this match. 🥱```")))

(defn ^:private estimated-minutes-left-in-match
  "Estimates how many minutes are left in the match, or nil if no such estimate can be made (e.g. when the match is not currently being played)."
  [match]
  (case (:status match)
    "IN_PLAY" (when-let [match-minute (:minute match)] (max 0 (- 90 (u/parse-int match-minute))))
    nil))

(defn post-match-summary-to-channel!
  [football-data-api-token
   discord-message-channel
   match-summary-channel-id
   country-to-channel-fn
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
                                      (chime/chime-at [(tm/plus (tm/instant) (tm/minutes retry-after))]
                                                      (partial post-match-summary-to-channel!
                                                               football-data-api-token
                                                               discord-message-channel
                                                               match-summary-channel-id
                                                               country-to-channel-fn
                                                               match-id
                                                               (inc retry-number))))
          ; Match is over; send summary message
          (or (= status "FINISHED")
              (= status "AWARDED")) (let [match-summary (str (get match-status-to-emoji (:status match) "❔")
                                                             "  **" (get-in match [:home-team :name] "Unknown") "** vs **" (get-in match [:away-team :name] "Unknown") "**, "
                                                             "final score: **" (get-in match [:score :full-time :home-team] (str shrug " ")) "-" (get-in match [:score :full-time :away-team] (str " " shrug)) "**")
                                          description   (str match-summary "\n"
                                                             (match-events-table match) "\n"
                                                             "Discuss in " (match-channel-link country-to-channel-fn match) ".")
                                          embed         (assoc (match-embed-template match)
                                                               :description description)]
                                      (log/info (str "Sending summary for match " match-id "..."))
                                      (mu/create-message! discord-message-channel
                                                          match-summary-channel-id
                                                          :embed embed)
                                      (log/info (str "Finished sending summary for match " match-id)))
          :else (log/warn (str "Unexpected status (" status ") for match " match-id "."))))
      (log/warn (str "Match " match-id " was not found by football-data.org. No summary message sent.")))
    (catch Exception e
      (u/log-exception e (str "Unexpected exception while sending summary for match " match-id)))))

(defn schedule-in-progress-match-summary!
  [football-data-api-token
   discord-message-channel
   match-summary-channel-id
   muted-leagues
   country-to-channel-fn
   match]
  (let [match-id     (:id match)
        match-league (get-in match [:competition :name])]
    (if-not (some (partial = match-league) muted-leagues)
      (let [post-match-summary-estimated-time (tm/plus (tm/instant) (tm/minutes (if-let [minutes-left (estimated-minutes-left-in-match match)]
                                                                                  minutes-left
                                                                                  1)))]
        (log/info (str "Scheduling summary for in-progress match " match-id "; first run will be at " post-match-summary-estimated-time))
        (chime/chime-at [post-match-summary-estimated-time]
                        (partial post-match-summary-to-channel!
                                 football-data-api-token
                                 discord-message-channel
                                 match-summary-channel-id
                                 country-to-channel-fn
                                 match-id
                                 0)))
      (log/info (str "In-progress match " match-id " is in a muted league - not scheduling a summary.")))))

(defn schedule-in-progress-match-summaries!
  "Schedules match summary jobs for all matches that are in progress at the time it's called."
  [football-data-api-token
   discord-message-channel
   match-summary-channel-id
   muted-leagues
   country-to-channel-fn]
  (if-let [in-progress-matches (fd/matches-in-play football-data-api-token)]
    (doall (map (partial schedule-in-progress-match-summary! football-data-api-token discord-message-channel match-summary-channel-id muted-leagues country-to-channel-fn) in-progress-matches))
    (log/info "No matches currently in progress; not scheduling any match summary jobs.")))

(defn post-match-reminder-to-channel!
  [football-data-api-token
   discord-message-channel
   match-reminder-duration
   match-reminder-channel-id
   country-to-channel-fn
   match-id & _]
  (try
    (log/info (str "Sending reminder for match " match-id "..."))
    (if-let [{match :match} (fd/match football-data-api-token match-id)]
      (let [starts-in-min      (try
                                 (.toMinutes (tm/duration (tm/zoned-date-time)
                                                          (u/in-tz "UTC" (tm/zoned-date-time (:utc-date match)))))
                                 (catch Exception e
                                   (.toMinutes ^java.time.Duration match-reminder-duration)))
            match-summary      (str (get match-status-to-emoji (:status match) "❔")
                                    "  **" (get-in match [:home-team :name] "Unknown") "** vs **" (get-in match [:away-team :name] "Unknown") "**")
            match-channel-link (match-channel-link country-to-channel-fn match)
            description        (case (:status match)
                                 "SCHEDULED" (str match-summary " starts in " starts-in-min " mins.\n\n"
                                                  "[Find out how to watch here](https://www.livesoccertv.com/), and discuss in " match-channel-link ".")
                                 "IN_PLAY"   (str match-summary ", which was originally due to start in " starts-in-min " mins, started early and is in progress.\n\n"
                                                  "[Find out how to watch here](https://www.livesoccertv.com/), and discuss in " match-channel-link ".")
                                 "FINISHED"  (str match-summary " has finished.\n\n"
                                                  "Discuss in " match-channel-link ".")
                                 (str match-summary ", which was due to start in " starts-in-min " minutes, has been " (s/lower-case (:status match)) ".\n"\n)
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
          (chime/chime-at [post-match-summary-estimated-time]
                          (partial post-match-summary-to-channel!
                                   football-data-api-token
                                   discord-message-channel
                                   match-reminder-channel-id
                                   country-to-channel-fn
                                   match-id
                                   0))))
      (log/warn (str "Match " match-id " was not found by football-data.org. No reminder message sent.")))
    (log/info (str "Finished sending reminder for match " match-id))
    (catch Exception e
      (u/log-exception e (str "Unexpected exception while sending reminder for match " match-id)))))

(defn schedule-match-reminder!
  "Schedules a reminder for the given match."
  [football-data-api-token
   discord-message-channel
   match-reminder-duration
   match-reminder-channel-id
   muted-leagues
   country-to-channel-fn
   match]
  (let [now           (u/in-tz "UTC" (tm/zoned-date-time))
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
                                   match-reminder-channel-id
                                   country-to-channel-fn
                                   (:id match))))
        (log/info (str "Reminder time " reminder-time " for match " (:id match) " has already passed - not scheduling a reminder.")))
      (log/info (str "Match " (:id match) " is in a muted league - not scheduling a reminder.")))))

(defn schedule-todays-match-reminders!
  "Schedules reminders for the remainder of today's matches."
  [football-data-api-token
   discord-message-channel
   match-reminder-duration
   match-reminder-channel-id
   muted-leagues
   country-to-channel-fn]
   (let [today                    (u/in-tz "UTC" (tm/zoned-date-time))
         todays-scheduled-matches (fd/scheduled-matches-on-day football-data-api-token today)]
     (if (seq todays-scheduled-matches)
       (doall (map (partial schedule-match-reminder! football-data-api-token
                                                     discord-message-channel
                                                     match-reminder-duration
                                                     match-reminder-channel-id
                                                     muted-leagues
                                                     country-to-channel-fn)
                   (distinct todays-scheduled-matches)))
       (log/info "No matches remaining today - not scheduling any reminders."))))

(def training-and-resources-discord-channel-link (mu/channel-link "686439362291826694"))

(defn check-for-new-dutch-referee-blog-quiz-and-post-to-channel!
  "Checks whether a new Dutch referee blog quiz has been posted in the last time-period-hours hours (defaults to 24), and posts it to the given channel if so."
  ([discord-message-channel channel-id] (check-for-new-dutch-referee-blog-quiz-and-post-to-channel! discord-message-channel channel-id 24))
  ([discord-message-channel
    channel-id
    time-period-hours]
   (if-let [new-quizzes (drb/quizzes (tm/minus (tm/instant) (tm/hours time-period-hours)))]
     (let [_          (log/info (str (count new-quizzes) " new Dutch referee blog quizz(es) found"))
           message    (str "<:dfb:753779768306040863> A new **Dutch Referee Blog Laws of the Game Quiz** has been posted: "
                           (:link (first new-quizzes))
                           "\nPuzzled by an answer? Click the react and we'll discuss in " training-and-resources-discord-channel-link "!")
           message-id (:id (mu/create-message! discord-message-channel
                                               channel-id
                                               :content message))]
       (if message-id
         (do
           (mu/create-reaction! discord-message-channel channel-id message-id "1️⃣")
           (mu/create-reaction! discord-message-channel channel-id message-id "2️⃣")
           (mu/create-reaction! discord-message-channel channel-id message-id "3️⃣")
           (mu/create-reaction! discord-message-channel channel-id message-id "4️⃣")
           (mu/create-reaction! discord-message-channel channel-id message-id "5️⃣"))
         (log/warn "No message id found for Dutch referee blog message - skipped adding reactions"))
       nil)
     (log/info "No new Dutch referee blog quizzes found"))))

(defn check-for-new-cnra-quiz-and-post-to-channel!
  "Checks whether any new CNRA quizzes has been posted in the last month, and posts them to the given channel if so."
  [discord-message-channel channel-id]
  (if-let [new-quizzes (cnra/quizzes (tm/minus (tm/local-date) (tm/months 1)))]
    (doall
      (map (fn [quiz]
        (let [message (str "<:cnra:769311341751959562> The **"
                           (:quiz-date quiz)
                           " CNRA Quiz** has been posted, on the topic of **"
                           (:topic quiz)
                           "**: "
                           (:link quiz)
                           "\nPuzzled by an answer? React and we'll discuss in " training-and-resources-discord-channel-link ".")]
          (mu/create-message! discord-message-channel
                              channel-id
                              :content message)))
         new-quizzes))
    (log/info "No new CNRA quizzes found")))

(def ist-youtube-channel-id              "UCmzFaEBQlLmMTWS0IQ90tgA")
(def memes-and-junk-discord-channel-link (mu/channel-link "683853455038742610"))

(defn post-youtube-video-to-channel!
  [discord-message-channel discord-channel-id youtube-channel-info-fn youtube-channel-id video]
  (let [channel-title (org.jsoup.parser.Parser/unescapeEntities (:title (youtube-channel-info-fn youtube-channel-id)) true)
        message       (str (:emoji (youtube-channel-info-fn youtube-channel-id))
                           (if channel-title (str " A new **" channel-title "** video has been posted: **") " A new video has been posted: **")
                           (org.jsoup.parser.Parser/unescapeEntities (:title video) true)
                           "**: https://www.youtube.com/watch?v=" (:id video)
                           "\nDiscuss in "
                           (if (= youtube-channel-id ist-youtube-channel-id) memes-and-junk-discord-channel-link training-and-resources-discord-channel-link)
                           ".")]
     (mu/create-message! discord-message-channel
                         discord-channel-id
                         :content message)))

(defn check-for-new-youtube-videos-and-post-to-channel!
  "Checks whether any new videos have been posted to the given YouTube channel in the last day, and posts it to the given Discord channel if so."
  [youtube-api-token discord-message-channel discord-channel-id youtube-channel-id youtube-channel-info-fn]
  (let [channel-title (:title (youtube-channel-info-fn youtube-channel-id))]
    (if-let [new-videos (yt/videos youtube-api-token
                                   (tm/minus (tm/instant) (tm/days 1))
                                   youtube-channel-id)]
      (doall (map (partial post-youtube-video-to-channel! discord-message-channel discord-channel-id youtube-channel-info-fn youtube-channel-id) new-videos))
      (log/info (str "No new videos found in YouTube channel " (if channel-title channel-title (str "-unknown (" youtube-channel-id ")-")))))))
