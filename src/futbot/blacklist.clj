;
; Copyright © 2021 Peter Monks
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

(ns futbot.blacklist
  (:require [clojure.tools.logging :as log]
            [futbot.config         :as cfg]
            [futbot.message-util   :as mu]))

(defn- check-blacklist-entry!
  [event-data re]
  (let [content (:content event-data)]
    (if (re-find re content)
      (let [message-id  (:id event-data)
            channel-id  (:channel-id event-data)
            author-id   (:id (:author event-data))
            author-name (mu/nick-or-user-name event-data)
            msg         (get cfg/blacklist re)]
        (log/info "Deleting message" message-id "sent by" author-id (str "(" author-name ")") "in channel" channel-id "as it matched blacklist entry" (str re))
        (mu/delete-message! cfg/discord-message-channel channel-id message-id)
        (mu/send-dm!        cfg/discord-message-channel author-id msg)

        ; Send admin message
        (mu/create-message! cfg/discord-message-channel
                            cfg/blacklist-notification-discord-channel-id
                            (str "**[BLACKLIST VIOLATION]** In <#" channel-id ">, <@" author-id "> wrote:\n>>> " content))
        true)
      false)))

(defn process!
  [event-data]
  (loop [f      (first cfg/blacklist-res)
         r      (rest  cfg/blacklist-res)
         result false]
    (if (and f (not result))
      (recur (first r)
             (rest r)
             (check-blacklist-entry! event-data f))
      result)))
