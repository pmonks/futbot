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
  (:require [clojure.tools.logging       :as log]
            [mount.core                  :as mnt :refer [defstate]]
            [java-time                   :as tm]
            [chime.core                  :as chime]
            [futbot.util                 :as u]
            [futbot.config               :as cfg]
            [futbot.source.football-data :as fd]
            [futbot.core                 :as core]))

(defn close-job
  "Closes a timed job defined by the defjob macro."
  [^java.lang.AutoCloseable job]
  (.close job))

(defmacro defjob
  "Defines a timed job."
  [name start interval & body]
  (let [job-name# (str name)]
    `(defstate ~name
       :start (let [~'start ~start]
                (log/info ~(str "Scheduling " job-name# "; first run will be at") (str ~'start))
                (chime/chime-at (chime/periodic-seq ~'start ~interval)
                                (fn [~'_]
                                  (try
                                    (log/info ~(str job-name# " started..."))
                                    ~@body
                                    (log/info ~(str job-name# " finished"))
                                    (catch clojure.lang.ExceptionInfo ~'ie
                                      (log/error ~'ie (str "Unexpected exception in " ~job-name# "; data: " (ex-data ~'ie))))
                                    (catch Exception ~'e
                                      (log/error ~'e ~(str "Unexpected exception in " job-name#)))))))
       :stop (close-job ~name))))

; Run the GC 7 minutes after startup, then every hour after that
(defjob gc-job
        (tm/plus (tm/instant) (tm/minutes 7))
        (tm/hours 1)
        (System/gc))

; Prepare the daily schedule at midnight UTC every day
(defjob daily-schedule-job
        (tm/plus (tm/truncate-to (tm/instant) :days) (tm/days 1))
        (tm/days 1)
        (let [today                    (u/in-tz "UTC" (tm/zoned-date-time))   ; Note: can't use a tm/instant here as football-data code doesn't support it (TODO)
              todays-scheduled-matches (fd/scheduled-matches-on-day cfg/football-data-api-token today)]
          (core/post-daily-schedule-to-channel! cfg/discord-message-channel
                                                cfg/daily-schedule-discord-channel-id
                                                (partial fd/match cfg/football-data-api-token)
                                                today
                                                todays-scheduled-matches)
          (core/schedule-todays-reminders! cfg/football-data-api-token
                                           cfg/discord-message-channel
                                           cfg/match-reminder-duration
                                           cfg/muted-leagues
                                           #(get cfg/country-to-channel % cfg/default-reminder-channel-id)
                                           cfg/referee-emoji
                                           todays-scheduled-matches)))

; Check for new Dutch Referee Blog Quizzes at 9am Amsterdam each day. This job runs in Europe/Amsterdam timezone, since that's where the Dutch Referee Blog is located
(defjob dutch-referee-blog-quiz-job
        (let [now              (u/in-tz "Europe/Amsterdam" (tm/zoned-date-time))
              today-at-nine-am (u/in-tz "Europe/Amsterdam" (tm/plus (tm/truncate-to now :days) (tm/hours 9)))]
          (if (tm/before? now today-at-nine-am)
            today-at-nine-am
            (tm/plus today-at-nine-am (tm/days 1))))
        (tm/days 1)
        (core/check-for-new-dutch-referee-blog-quiz-and-post-to-channel! cfg/discord-message-channel cfg/quiz-channel-id))

; Check for new CNRA Quizzes at midnight Los Angeles on the 16th of the month. This job runs in America/Los_Angeles timezone, since that's where CNRA is located
(defjob cnra-quiz-job
        (let [now                                (u/in-tz "America/Los_Angeles" (tm/zoned-date-time))
; ####TEST TO FORCE CNRA JOB TO RUN AGAIN ON 2020-11-17!
              sixteenth-of-the-month-at-midnight (u/in-tz "America/Los_Angeles" (tm/plus (tm/truncate-to (tm/adjust now :first-day-of-month) :days) (tm/days 16)))]
;              sixteenth-of-the-month-at-midnight (u/in-tz "America/Los_Angeles" (tm/plus (tm/truncate-to (tm/adjust now :first-day-of-month) :days) (tm/days 15)))]
          (if (tm/before? now sixteenth-of-the-month-at-midnight)
            sixteenth-of-the-month-at-midnight
            (tm/plus sixteenth-of-the-month-at-midnight (tm/months 1))))
        (tm/months 1)
        (core/check-for-new-cnra-quiz-and-post-to-channel! cfg/discord-message-channel cfg/quiz-channel-id))


; Youtube jobs are a bit messy, since the total number is defined in config, not hardcoded as the jobs above are
(defn schedule-youtube-job
  [job-time youtube-channel-id]
  (let [youtube-channel-name (get-in cfg/youtube-channels-info [youtube-channel-id :title] (str "-unknown (" youtube-channel-id ")-"))]
    (log/info (str "Scheduling Youtube channel " youtube-channel-name " job; first run will be at " job-time))
    (chime/chime-at (chime/periodic-seq job-time (tm/days 1))
                    (fn [_]
                      (try
                        (log/info (str "Youtube channel " youtube-channel-name " job started..."))
                        (core/check-for-new-youtube-video-and-post-to-channel! cfg/youtube-api-token
                                                                               cfg/discord-message-channel
                                                                               cfg/video-channel-id
                                                                               youtube-channel-id
                                                                               cfg/youtube-channels-info)
                        (log/info (str "Youtube channel " youtube-channel-name " job finished"))
                        (catch clojure.lang.ExceptionInfo ie
                          (log/error ie (str "Unexpected exception in Youtube channel " youtube-channel-name " job; data: " (ex-data ie))))
                        (catch Exception e
                          (log/error e (str "Unexpected exception in Youtube channel " youtube-channel-name " job"))))))))

; Each Youtube job is run once per day, and they're equally spaced throughout the day to spread out the load
(defstate youtube-jobs
          :start (let [interval (int (/ (* 24 60) (count cfg/youtube-channels)))
                       now      (tm/instant)
                       midnight (tm/truncate-to now :days)]
                   (loop [f      (first cfg/youtube-channels)
                          r      (rest cfg/youtube-channels)
                          index  1
                          result []]
                     (let [job-time (tm/plus midnight (tm/minutes (* index interval)))
                           job-time (if (tm/before? job-time now)
                                      (tm/plus job-time (tm/days 1))
                                      job-time)]
                       (if-not f
                         result
                         (recur (first r)
                                (rest r)
                                (inc index)
                                (conj result (schedule-youtube-job (tm/instant job-time) f)))))))
          :stop (doall (map #(.close ^java.lang.AutoCloseable %) youtube-jobs)))
