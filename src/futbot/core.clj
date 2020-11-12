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

(defn- close-job
  "Closes a timed job defined by the defjob macro."
  [^java.lang.AutoCloseable job]
  (.close job))

(defmacro defjob
  "Defines a timed job."
  [name start recurrence & body]
  (let [job-name# (str name)]
    `(mount.core/defstate ~name
       :start (let [~'start ~start]
                (log/info ~(str "Scheduling " job-name# "; first run will be at") (str ~'start))
                (chime/chime-at (chime/periodic-seq ~'start ~recurrence)
                                (fn [~'_]
                                  (try
                                    (log/info ~(str job-name# " started..."))
                                    ~@body
                                    (log/info ~(str job-name# " finished"))
                                    (catch Exception ~'e
                                      (log/error ~'e ~(str "Unexpected exception in " job-name#)))))))
       :stop (close-job ~name))))

(defjob gc-job
        (tm/instant (tm/with-clock (tm/system-clock "UTC") (tm/plus (tm/truncate-to (tm/zoned-date-time) :hours) (tm/minutes 67))))
        (tm/hours 1)
        (System/gc))

(defjob daily-schedule-job
        (tm/with-clock (tm/system-clock "UTC") (tm/truncate-to (tm/plus (tm/zoned-date-time) (tm/days 1)) :days))
        (tm/days 1)
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
                                          todays-scheduled-matches)))

(defjob dutch-referee-blog-quiz-job
        (let [now              (tm/with-clock (tm/system-clock "Europe/Amsterdam") (tm/zoned-date-time))
              today-at-nine-am (tm/with-clock (tm/system-clock "Europe/Amsterdam") (tm/plus (tm/truncate-to now :days) (tm/hours 9)))]
          (if (tm/before? now today-at-nine-am)
            today-at-nine-am
            (tm/plus today-at-nine-am (tm/days 1))))
        (tm/days 1)
        (job/check-for-new-dutch-referee-blog-quiz-and-post-to-channel! cfg/discord-message-channel
                                                                        cfg/quiz-channel-id))

(defjob cnra-quiz-job
        (let [now                                (tm/with-clock (tm/system-clock "America/Los_Angeles") (tm/zoned-date-time))
              sixteenth-of-the-month-at-midnight (tm/with-clock (tm/system-clock "America/Los_Angeles") (tm/truncate-to (tm/plus (tm/adjust now :first-day-of-month) (tm/days 15)) :days))]
          (if (tm/before? now sixteenth-of-the-month-at-midnight)
            sixteenth-of-the-month-at-midnight
            (tm/plus sixteenth-of-the-month-at-midnight (tm/months 1))))
        (tm/months 1)
        (job/check-for-new-cnra-quiz-and-post-to-channel! cfg/discord-message-channel
                                                          cfg/quiz-channel-id))


; Youtube jobs are a bit messy, since the total number is defined in config, not hardcoded as the jobs above are
(defn schedule-youtube-job
  [job-time youtube-channel-id]
  (let [youtube-channel-name (get-in cfg/youtube-channels-info [youtube-channel-id :title] (str "-unknown (" youtube-channel-id ")-"))]
    (log/info (str "Scheduling Youtube channel " youtube-channel-name " job; first run will be at " job-time))
    (chime/chime-at (chime/periodic-seq job-time (tm/days 1))
                    (fn [_]
                      (try
                        (log/info (str "Youtube channel " youtube-channel-name " job started..."))
                        (job/check-for-new-youtube-video-and-post-to-channel! cfg/youtube-api-token
                                                                              cfg/discord-message-channel
                                                                              cfg/video-channel-id
                                                                              youtube-channel-id
                                                                              cfg/youtube-channels-info)
                        (log/info (str "Youtube channel " youtube-channel-name " job finished"))
                        (catch Exception e
                          (log/error e (str "Unexpected exception in Youtube channel " youtube-channel-name " job"))))))))

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
                                (conj result (schedule-youtube-job (tm/instant job-time) f)))))))
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
