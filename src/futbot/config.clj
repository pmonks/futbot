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

(ns futbot.config
  (:require [clojure.java.io       :as io]
            [clojure.string        :as s]
            [clojure.tools.logging :as log]
            [clojure.edn           :as edn]
            [clojure.core.async    :as async]
            [java-time             :as tm]
            [aero.core             :as a]
            [mount.core            :as mnt :refer [defstate]]
            [discljord.connections :as dc]
            [discljord.messaging   :as dm]
            [futbot.util           :as u]
            [futbot.source.youtube :as yt]))

; Because java.util.logging is a hot mess
(org.slf4j.bridge.SLF4JBridgeHandler/removeHandlersForRootLogger)
(org.slf4j.bridge.SLF4JBridgeHandler/install)

; Because Java's default exception behaviour in threads other than main is a hot mess
(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ t e]
     (u/log-exception e (str "Uncaught exception on " (.getName t))))))

(def boot-time (tm/instant))

; Adds a #split reader macro to aero - see https://github.com/juxt/aero/issues/55
(defmethod a/reader 'split
  [_ _ value]
  (let [[s re] value]
    (when (and s re)
      (s/split s (re-pattern re)))))

(defstate config
          :start (if-let [config-file (:config-file (mnt/args))]
                   (a/read-config config-file)
                   (a/read-config (io/resource "config.edn"))))


(defstate football-data-api-token
          :start (let [token (:football-data-api-token config)]
                   (if-not (s/blank? token)
                     token
                     (throw (ex-info "football-data.org API token not provided" {})))))

(defstate discord-api-token
          :start (let [token (:discord-api-token config)]
                   (if-not (s/blank? token)
                     token
                     (throw (ex-info "Discord API token not provided" {})))))

(defstate discord-event-channel
          :start (async/chan (:discord-event-channel-size config))
          :stop  (async/close! discord-event-channel))

(defstate discord-connection-channel
          :start (if-let [connection (dc/connect-bot! discord-api-token discord-event-channel :intents #{:guilds :guild-messages :direct-messages})]
                   connection
                   (throw (ex-info "Failed to connect bot to Discord" {})))
          :stop  (dc/disconnect-bot! discord-connection-channel))

(defstate discord-message-channel
          :start (if-let [connection (dm/start-connection! discord-api-token)]
                   connection
                   (throw (ex-info "Failed to connect to Discord message channel" {})))
          :stop  (dm/stop-connection! discord-message-channel))

(defstate daily-schedule-discord-channel-id
          :start (let [channel-id (:daily-schedule-discord-channel-id config)]
                   (if-not (s/blank? channel-id)
                     channel-id
                     (throw (ex-info "Daily schedule Discord channel id not provided" {})))))

(defstate match-reminder-duration
          :start (tm/minutes (if-let [duration (:match-reminder-duration-mins config)]
                               duration
                               15)))  ; Default is 15 minutes

(defstate country-to-channel
          :start (:country-to-channel-map config))

(defstate default-reminder-channel-id
          :start (let [channel-id (:default-reminder-channel-id config)]
                   (if-not (s/blank? channel-id)
                     channel-id
                     (throw (ex-info "Default country Discord channel id not provided" {})))))

(defstate muted-leagues
          :start (:muted-leagues config))

(defstate referee-emoji
          :start (:referee-emoji config))

(defstate quiz-channel-id
          :start (let [channel-id (:quiz-channel-id config)]
                   (if-not (s/blank? channel-id)
                     channel-id
                     (throw (ex-info "Quiz Discord channel id not provided" {})))))

(defstate video-channel-id
          :start (let [channel-id (:video-channel-id config)]
                   (if-not (s/blank? channel-id)
                     channel-id
                     (throw (ex-info "Video Discord channel id not provided" {})))))

(defstate ist-channel-ids
          :start (:ist-channel-ids config))

(defstate youtube-api-token
          :start (let [token (:youtube-api-token config)]
                   (if-not (s/blank? token)
                     token
                     (throw (ex-info "YouTube API token not provided" {})))))

(defstate default-youtube-emoji
          :start (:default-youtube-emoji config))

(defstate youtube-channels-emoji
          :start (:youtube-channels config))

(defstate youtube-channels
          :start (keys youtube-channels-emoji))

(defstate youtube-channels-info
          :start (apply assoc nil (mapcat (fn [youtube-channel-id]
                                            [youtube-channel-id (into {:emoji (u/getrn youtube-channels-emoji youtube-channel-id default-youtube-emoji)}
                                                                      (try
                                                                        (yt/channel-info youtube-api-token youtube-channel-id)
                                                                        (catch Exception e
                                                                          (log/warn e (str "Error retrieving YouTube channel info for " youtube-channel-id
                                                                                           ": status code=" (:status (ex-data e))
                                                                                           ", message="     (:message (:error (:body (ex-data e))))))
                                                                          nil)))])
                                          youtube-channels)))

; Note: do NOT use mount for this, since it's used before mount has started
(def ^:private build-info
  (if-let [deploy-info (io/resource "deploy-info.edn")]
    (edn/read-string (slurp deploy-info))
    (throw (RuntimeException. "deploy-info.edn classpath resource not found - did you remember to run the 'git-info-edn' alias first?"))))

(def git-revision
  (s/trim (:hash build-info)))

(def git-url
  (when-not (s/blank? git-revision)
    (str "https://github.com/pmonks/futbot/tree/" git-revision)))

(def built-at
  (tm/instant (:date build-info)))
