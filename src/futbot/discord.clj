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

(ns futbot.discord
  (:require [clojure.string        :as s]
            [clojure.tools.logging :as log]
            [mount.core            :as mnt :refer [defstate]]
            [futbot.config         :as cfg]
            [clojure.core.async    :as async]
            [discljord.connections :as dic]
            [discljord.messaging   :as dim]))

(defstate token
          :start (:discord-api-token cfg/config))

(defstate event-channel
          :start (async/chan (:discord-event-channel-size cfg/config))
          :stop  (async/close! event-channel))

(defstate connection-channel
          :start (dic/connect-bot! token event-channel)
          :stop  (dic/disconnect-bot! connection-channel))

(defstate message-channel
          :start (dim/start-connection! token)
          :stop  (dim/stop-connection! message-channel))

(defn send-message
  ([msg] (send-message msg nil))
  ([msg attachment]
    (println "####TODO: IMPLEMENT ME!")))
