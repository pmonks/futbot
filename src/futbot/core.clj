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
  (:require [clojure.tools.logging       :as log]
            [mount.core                  :as mnt :refer [defstate]]
            [java-time                   :as tm]
            [chime.core                  :as chime]
            [discljord.events            :as de]
            [futbot.config               :as cfg]
            [futbot.source.football-data :as fd]
            [futbot.jobs                 :as job]
            [futbot.chat                 :as chat]))

(defstate gc-job
          :start (let [seven-past-the-hour-UTC               (tm/with-clock (tm/system-clock "UTC") (tm/plus (tm/truncate-to (tm/zoned-date-time) :hours) (tm/minutes 7)))
                       every-hour-at-seven-past-the-hour-UTC (chime/periodic-seq (tm/instant seven-past-the-hour-UTC)
                                                                                 (tm/duration 1 :hours))]
                   (log/info (str "Scheduling GC job; first run will be at " (first every-hour-at-seven-past-the-hour-UTC)))
                   (chime/chime-at every-hour-at-seven-past-the-hour-UTC
                                   (fn [_] (System/gc))))
          :stop (.close ^java.lang.AutoCloseable gc-job))

(defstate daily-schedule-job
          :start (let [tomorrow-at-midnight-UTC  (tm/with-clock (tm/system-clock "UTC") (tm/truncate-to (tm/plus (tm/zoned-date-time) (tm/days 1)) :days))
                       every-day-at-midnight-UTC (chime/periodic-seq (tm/instant tomorrow-at-midnight-UTC)
                                                                     (tm/period 1 :days))]
                   (log/info (str "Scheduling daily schedule job; first run will be at " (first every-day-at-midnight-UTC)))
                   (chime/chime-at every-day-at-midnight-UTC
                                   (fn [_]
                                     (try
                                       (log/info "Daily schedule job started...")
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
                                                                         todays-scheduled-matches))
                                       (catch Exception e
                                         (log/error e "Unexpected exception in daily schedule job"))
                                       (finally
                                         (log/info "Daily schedule job finished"))))))
          :stop (.close ^java.lang.AutoCloseable daily-schedule-job))

; This job runs in Europe/Amsterdam timezone, since that's where the Dutch Referee Blog is located
(defstate dutch-referee-blog-quiz-job
          :start (let [next-six-pm         (let [now             (tm/with-clock (tm/system-clock "Europe/Amsterdam") (tm/zoned-date-time))
                                                 today-at-six-pm (tm/with-clock (tm/system-clock "Europe/Amsterdam") (tm/plus (tm/truncate-to now :days) (tm/hours 18)))]
                                             (if (tm/before? now today-at-six-pm)
                                               today-at-six-pm
                                               (tm/plus today-at-six-pm (tm/days 1))))
                       every-day-at-six-pm (chime/periodic-seq next-six-pm
                                                              (tm/period 1 :days))]
                   (log/info (str "Scheduling Dutch referee blog quiz job; first run will be at " (first every-day-at-six-pm)))
                   (chime/chime-at every-day-at-six-pm
                                   (fn [_]
                                     (try
                                       (log/info "Dutch referee blog quiz job started...")
                                       (job/check-for-new-dutch-referee-blog-quiz-and-post-to-channel! cfg/discord-message-channel
                                                                                                       cfg/quiz-channel-id)
                                       (catch Exception e
                                         (log/error e "Unexpected exception in Dutch referee blog quiz job"))
                                       (finally
                                         (log/info "Dutch referee blog quiz job finished"))))))
          :stop (.close ^java.lang.AutoCloseable dutch-referee-blog-quiz-job))

; This job runs in America/Los_Angeles timezone, since that's where CNRA is located
(defstate cnra-quiz-job
          :start (let [next-16th-of-the-month-at-midnight  (let [now                                (tm/with-clock (tm/system-clock "America/Los_Angeles") (tm/zoned-date-time))
                                                                 sixteenth-of-the-month-at-midnight (tm/with-clock (tm/system-clock "America/Los_Angeles") (tm/truncate-to (tm/plus (tm/adjust now :first-day-of-month) (tm/days 15)) :days))]
                                                             (if (tm/before? now sixteenth-of-the-month-at-midnight)
                                                               sixteenth-of-the-month-at-midnight
                                                               (tm/plus sixteenth-of-the-month-at-midnight (tm/months 1))))
                       sixteenth-of-every-month-at-midnight (chime/periodic-seq next-16th-of-the-month-at-midnight
                                                                                (tm/period 1 :months))]
                   (log/info (str "Scheduling CNRA quiz job; first run will be at " (first sixteenth-of-every-month-at-midnight)))
                   (chime/chime-at sixteenth-of-every-month-at-midnight
                                   (fn [_]
                                     (try
                                       (log/info "CNRA quiz job started...")
                                       (job/check-for-new-cnra-quiz-and-post-to-channel! cfg/discord-message-channel
                                                                                         cfg/quiz-channel-id)
                                       (catch Exception e
                                         (log/error e "Unexpected exception in CNRA quiz job"))
                                       (finally
                                         (log/info "CNRA quiz job finished"))))))
          :stop (.close ^java.lang.AutoCloseable cnra-quiz-job))

(defn schedule-youtube-job
  [job-time youtube-channel-id]
  (let [youtube-channel-name (get-in cfg/youtube-channels-info [youtube-channel-id :title] (str "-unknown (" youtube-channel-id ")-"))]
    (log/info (str "Scheduling Youtube channel " youtube-channel-name " job; first run will be at " job-time))
    (chime/chime-at (chime/periodic-seq job-time (tm/period 1 :days))
                    (fn [_]
                      (try
                        (log/info (str "Youtube channel " youtube-channel-name " job started..."))
                        (job/check-for-new-youtube-video-and-post-to-channel! cfg/youtube-api-token
                                                                              cfg/discord-message-channel
                                                                              cfg/video-channel-id
                                                                              youtube-channel-id
                                                                              cfg/youtube-channels-info)
                        (catch Exception e
                          (log/error e (str "Unexpected exception in Youtube channel " youtube-channel-name " job")))
                        (finally
                          (log/info (str "Youtube channel " youtube-channel-name " job finished"))))))))

(defstate youtube-jobs
          :start (let [interval     (int (/ (* 24 60) (count cfg/youtube-channels)))
                       now-UTC      (tm/with-clock (tm/system-clock "UTC") (tm/zoned-date-time))
                       midnight-UTC (tm/truncate-to now-UTC :days)]
                   (loop [f      (first cfg/youtube-channels)
                          r      (rest cfg/youtube-channels)
                          index  1
                          result []]
                     (let [job-time (tm/plus midnight-UTC (tm/minutes (* index interval)))
                           job-time (if (tm/before? job-time now-UTC)
                                      (tm/plus job-time (tm/days 1))
                                      job-time)]
                       (if-not f
                         result
                         (recur (first r)
                                (rest r)
                                (inc index)
                                (conj result (schedule-youtube-job job-time f)))))))
          :stop (doall (map #(.close ^java.lang.AutoCloseable %) youtube-jobs)))


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
