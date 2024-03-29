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
  (:require [clojure.tools.logging :as log]
            [mount.core            :as mnt :refer [defstate]]
            [java-time             :as tm]
            [chime.core            :as chime]
            [futbot.util           :as u]
            [futbot.config         :as cfg]
            [futbot.matches        :as match]
            [futbot.quizzes        :as quiz]
            [futbot.videos         :as vid]
            [futbot.posts          :as post]))

(defn close-job
  "Closes a timed job defined by the defjob macro."
  [^java.lang.AutoCloseable job]
  (.close job))

(defmacro defjob
  "Defines a timed job."
  [name start interval & body]
  `(defstate ~name
     :start (let [~'start ~start]    ; Evaluate and cache 'start', since it may be an expensive computation
              (log/info ~(str "Scheduling " name "; first run will be at") (str ~'start))
              (chime/chime-at (chime/periodic-seq ~'start ~interval)
                              (fn [~'_]
                                (try
                                  (log/info ~(str name " started..."))
                                  ~@body
                                  (log/info ~(str name " finished"))
                                  (catch Exception ~'e
                                    (u/log-exception ~'e ~(str "Unexpected exception in " name)))))))
     :stop (close-job ~name)))

; Run the GC 2 minutes after startup, then every hour after that
(declare gc-job)
(defjob  gc-job
         (tm/plus (tm/instant) (tm/minutes 2))
         (tm/hours 1)
         (System/gc))

; Schedule match reminders for the day
(declare schedule-match-reminders-job)
(defjob  schedule-match-reminders-job
         (tm/plus (tm/truncate-to (tm/instant) :days) (tm/days 1))
         (tm/days 1)
         (match/schedule-todays-match-reminders! cfg/config))

; Check for new Dutch Referee Blog Quizzes at 9am Amsterdam each day. This job runs in Europe/Amsterdam timezone, since that's where the Dutch Referee Blog is located
(declare dutch-referee-blog-quiz-job)
(defjob  dutch-referee-blog-quiz-job
         (let [now              (u/in-tz "Europe/Amsterdam" (tm/zoned-date-time))
               today-at-nine-am (u/in-tz "Europe/Amsterdam" (tm/plus (tm/truncate-to now :days) (tm/hours 9)))]
           (if (tm/before? now today-at-nine-am)
             today-at-nine-am
             (tm/plus today-at-nine-am (tm/days 1))))
         (tm/days 1)
         (quiz/check-for-new-dutch-referee-blog-quiz-and-post! cfg/config))

; Check for new CNRA Quizzes at midnight Los Angeles on the 16th of the month. This job runs in America/Los_Angeles timezone, since that's where CNRA is located
(declare cnra-quiz-job)
(defjob  cnra-quiz-job
         (let [now                                (u/in-tz "America/Los_Angeles" (tm/zoned-date-time))
               sixteenth-of-the-month-at-midnight (u/in-tz "America/Los_Angeles" (tm/plus (tm/truncate-to (tm/adjust now :first-day-of-month) :days) (tm/days 15)))]
           (if (tm/before? now sixteenth-of-the-month-at-midnight)
             sixteenth-of-the-month-at-midnight
             (tm/plus sixteenth-of-the-month-at-midnight (tm/months 1))))
         (tm/months 1)
         (quiz/check-for-new-cnra-quiz-and-post! cfg/config))

; Check for new PRO posts at 9am New York each day. This job runs in America/New_York timezone, since that's where PRO is located
(declare pro-job)
(defjob  pro-job
         (let [now              (u/in-tz "America/New_York" (tm/zoned-date-time))
               today-at-nine-am (u/in-tz "America/New_York" (tm/plus (tm/truncate-to now :days) (tm/hours 9)))]
           (if (tm/before? now today-at-nine-am)
             today-at-nine-am
             (tm/plus today-at-nine-am (tm/days 1))))
         (tm/days 1)
         (post/check-for-new-pro-posts-and-post! cfg/config))

; YouTube jobs are a bit messy, since the total number is defined in config, not hardcoded as the jobs above are
(defn schedule-youtube-job
  [job-time youtube-channel-id]
  (let [youtube-channel-name (get-in cfg/config [:youtube-channels youtube-channel-id :title] (str "-unknown (" youtube-channel-id ")-"))]
    (log/info (str "Scheduling YouTube channel " youtube-channel-name " job; first run will be at " job-time))
    (chime/chime-at (chime/periodic-seq job-time (tm/days 1))
                    (fn [_]
                      (try
                        (log/info (str "YouTube channel " youtube-channel-name " job started..."))
                        (vid/check-for-new-youtube-videos-and-post! cfg/config
                                                                    youtube-channel-id)
                        (log/info (str "YouTube channel " youtube-channel-name " job finished"))
                        (catch Exception e
                          (u/log-exception e (str "Unexpected exception in YouTube channel " youtube-channel-name " job"))))))))

; Each YouTube job is run once per day, and they're equally spaced throughout the day to spread out the load
(declare  youtube-jobs)
(defstate youtube-jobs
          :start (let [youtube-channel-ids (keys (:youtube-channels cfg/config))
                       interval            (int (/ (* 24 60) (count youtube-channel-ids)))
                       now                 (tm/instant)
                       midnight            (tm/truncate-to now :days)]
                   (loop [f      (first youtube-channel-ids)
                          r      (rest  youtube-channel-ids)
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
