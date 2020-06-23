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
  (:require [clojure.java.io       :as io]
            [clojure.core.async    :as async]
            [mount.core            :as mnt :refer [defstate]]
            [futbot.config         :as cfg]
            [java-time             :as tm]
            [chime.core            :as chime]
            [futbot.football-data  :as fd]
            [futbot.template       :as tem]
            [discljord.connections :as dic]
            [discljord.messaging   :as dim]))

(defstate football-data-api-token
          :start (:football-data-api-token cfg/config))

(defstate discord-api-token
          :start (:discord-api-token cfg/config))

(defstate discord-event-channel
          :start (async/chan (:discord-event-channel-size cfg/config))
          :stop  (async/close! discord-event-channel))

(defstate discord-connection-channel
          :start (dic/connect-bot! discord-api-token discord-event-channel)
          :stop  (dic/disconnect-bot! discord-connection-channel))

(defstate discord-message-channel
          :start (dim/start-connection! discord-api-token)
          :stop  (dim/stop-connection! discord-message-channel))

(defstate daily-schedule-discord-channel-id
          :start (:daily-schedule-discord-channel-id cfg/config))

(def tomorrow-at-midnight-UTC  (tm/with-clock (tm/system-clock "UTC") (tm/truncate-to (tm/plus (tm/zoned-date-time) (tm/days 1)) :days)))
(def every-day-at-midnight-UTC (chime/periodic-seq (tm/instant tomorrow-at-midnight-UTC)
                                                   (tm/period 1 :days)))

(defn post-daily-schedule!
  "Generates and posts the daily-schedule (as an HTML attachment) to the Discord channel identified by daily-schedule-discord-channel-id."
  []
  (try
    (let [now                 (tm/with-clock (tm/system-clock "UTC") (tm/zoned-date-time))
          now-date-str        (tm/format "yyyy-MM-dd" now)
          todays-matches      (fd/matches-on-day football-data-api-token now)]
      (if (seq todays-matches)
        (let [daily-schedule-html (tem/render "daily-schedule.ftl"
                                              {:day     now-date-str
                                               :matches todays-matches})]
          (dim/create-message! discord-message-channel
                               daily-schedule-discord-channel-id
                               :content (str "Scheduled matches for: " now-date-str)
                               :stream {:content (io/input-stream daily-schedule-html) :filename (str "daily-schedule-" now-date-str ".html")}))
        (dim/create-message! discord-message-channel
                             daily-schedule-discord-channel-id
                             :content (str "Sadly there are no ⚽️ matches scheduled for today (" now-date-str "). 😢"))))))

(defstate daily-schedule-job
          :start (chime/chime-at every-day-at-midnight-UTC
                                 post-daily-schedule!)
          :stop (.close ^java.lang.AutoCloseable daily-schedule-job))
