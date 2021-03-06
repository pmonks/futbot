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
  (:require [clojure.java.io        :as io]
            [clojure.string         :as s]
            [clojure.tools.logging  :as log]
            [clojure.edn            :as edn]
            [clojure.core.async     :as async]
            [java-time              :as tm]
            [aero.core              :as a]
            [mount.core             :as mnt :refer [defstate]]
            [org.httpkit.client     :as http]
            [org.httpkit.sni-client :as sni-client]
            [discljord.connections  :as dc]
            [discljord.messaging    :as dm]
            [futbot.util            :as u]
            [futbot.source.youtube  :as yt]))

; Because java.util.logging is a hot mess
(org.slf4j.bridge.SLF4JBridgeHandler/removeHandlersForRootLogger)
(org.slf4j.bridge.SLF4JBridgeHandler/install)

; Because Java's default exception behaviour in threads other than main is a hot mess
(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ t e]
     (u/log-exception e (str "Uncaught exception on " (.getName t))))))

; See https://github.com/http-kit/http-kit#enabling-client-sni-support-disabled-by-default
(alter-var-root #'http/*default-client* (fn [_] sni-client/default-client))

(def boot-time (tm/instant))

; Adds a #split reader macro to aero - see https://github.com/juxt/aero/issues/55
(defmethod a/reader 'split
  [_ _ value]
  (let [[s re] value]
    (when (and s re)
      (s/split s (re-pattern re)))))

(declare config)
(defstate config
          :start (if-let [config-file (:config-file (mnt/args))]
                   (a/read-config config-file)
                   (a/read-config (io/resource "config.edn"))))


(declare football-data-api-token)
(defstate football-data-api-token
          :start (let [token (:football-data-api-token config)]
                   (if-not (s/blank? token)
                     token
                     (throw (ex-info "football-data.org API token not provided" {})))))

(declare discord-api-token)
(defstate discord-api-token
          :start (let [token (:discord-api-token config)]
                   (if-not (s/blank? token)
                     token
                     (throw (ex-info "Discord API token not provided" {})))))

(declare discord-event-channel)
(defstate discord-event-channel
          :start (async/chan (:discord-event-channel-size config))
          :stop  (async/close! discord-event-channel))

(declare discord-connection-channel)
(defstate discord-connection-channel
          :start (if-let [connection (dc/connect-bot! discord-api-token discord-event-channel :intents #{:guilds :guild-messages :direct-messages})]
                   connection
                   (throw (ex-info "Failed to connect bot to Discord" {})))
          :stop  (dc/disconnect-bot! discord-connection-channel))

(declare discord-message-channel)
(defstate discord-message-channel
          :start (if-let [connection (dm/start-connection! discord-api-token)]
                   connection
                   (throw (ex-info "Failed to connect to Discord message channel" {})))
          :stop  (dm/stop-connection! discord-message-channel))

(declare match-reminder-discord-channel-id)
(defstate match-reminder-discord-channel-id
          :start (let [channel-id (:match-reminder-discord-channel-id config)]
                   (if-not (s/blank? channel-id)
                     channel-id
                     (throw (ex-info "Match reminder Discord channel id not provided" {})))))

(declare match-reminder-duration)
(defstate match-reminder-duration
          :start (tm/minutes (if-let [duration (:match-reminder-duration-mins config)]
                               duration
                               15)))  ; Default is 15 minutes

(declare country-to-channel)
(defstate country-to-channel
          :start (:country-to-channel-map config))

(declare default-reminder-channel-id)
(defstate default-reminder-channel-id
          :start (let [channel-id (:default-reminder-channel-id config)]
                   (if-not (s/blank? channel-id)
                     channel-id
                     (throw (ex-info "Default country Discord channel id not provided" {})))))

(declare muted-leagues)
(defstate muted-leagues
          :start (:muted-leagues config))

(declare quiz-channel-id)
(defstate quiz-channel-id
          :start (let [channel-id (:quiz-channel-id config)]
                   (if-not (s/blank? channel-id)
                     channel-id
                     (throw (ex-info "Quiz Discord channel id not provided" {})))))

(declare video-channel-id)
(defstate video-channel-id
          :start (let [channel-id (:video-channel-id config)]
                   (if-not (s/blank? channel-id)
                     channel-id
                     (throw (ex-info "Video Discord channel id not provided" {})))))

(declare ist-channel-ids)
(defstate ist-channel-ids
          :start (:ist-channel-ids config))

(declare youtube-api-token)
(defstate youtube-api-token
          :start (let [token (:youtube-api-token config)]
                   (if-not (s/blank? token)
                     token
                     (throw (ex-info "YouTube API token not provided" {})))))

(declare default-youtube-emoji)
(defstate default-youtube-emoji
          :start (:default-youtube-emoji config))

(declare youtube-channels-emoji)
(defstate youtube-channels-emoji
          :start (:youtube-channels config))

(declare youtube-channels)
(defstate youtube-channels
          :start (keys youtube-channels-emoji))

(declare youtube-channels-info)
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

(declare blocklist)
(defstate blocklist
          :start (u/mapfonk re-pattern (:blocklist config)))   ; Pre-compile all regexes as we load them from the config file

(declare blocklist-res)
(defstate blocklist-res
          :start (keys blocklist))

(declare blocklist-notification-discord-channel-id)
(defstate blocklist-notification-discord-channel-id
          :start (:blocklist-notification-discord-channel-id config))


; Note: do NOT use mount for these, since they're used before mount has started
(def ^:private build-info
  (if-let [deploy-info (io/resource "deploy-info.edn")]
    (edn/read-string (slurp deploy-info))
    (throw (RuntimeException. "deploy-info.edn classpath resource not found."))))

(def git-tag
  (s/trim (:tag build-info)))

(def git-url
  (when-not (s/blank? git-tag)
    (str "https://github.com/pmonks/futbot/tree/" git-tag)))

(def built-at
  (tm/instant (:date build-info)))

(defn set-log-level!
  "Sets the log level (which can be a string or a keyword) of the bot, for the given logger aka 'package' (a String, use 'ROOT' for the root logger)."
  [level ^String logger-name]
  (when (and level logger-name)
    (let [logger    ^ch.qos.logback.classic.Logger (org.slf4j.LoggerFactory/getLogger logger-name)                       ; This will always return a Logger object, even if it isn't used
          level-obj                                (ch.qos.logback.classic.Level/toLevel (s/upper-case (name level)))]   ; Note: this code defaults to DEBUG if the given level string isn't valid
      (.setLevel logger level-obj))))

(defn reset-logging!
  "Resets all log levels to their configured defaults."
  []
  (let [lc  ^ch.qos.logback.classic.LoggerContext (org.slf4j.LoggerFactory/getILoggerFactory)
        ci  (ch.qos.logback.classic.util.ContextInitializer. lc)
        url (.findURLOfDefaultConfigurationFile ci true)]
    (.reset lc)
    (.configureByResource ci url)))
