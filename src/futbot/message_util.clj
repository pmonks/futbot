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

(ns futbot.message-util
  (:require [clojure.tools.logging :as log]
            [discljord.messaging   :as dm]))

(defn- check-response-and-throw
  [response]
  (if (= (class response) clojure.lang.ExceptionInfo)
    (if (:retry-after (ex-data response))
      (log/info "Discord API call was rate limited (retry is automatic)")
      (throw response))
    response))

(defn create-message!
  "A version of discljord.message/create-message! that hides some of the API complexity, and (more importantly) throws errors."
  ([discord-message-channel channel-id message] (create-message! discord-message-channel channel-id message nil nil))
  ([discord-message-channel channel-id message file-is filename]
   (log/debug "Sending message to Discord channel" (str channel-id ":") message)
   (check-response-and-throw (if file-is
                               @(dm/create-message! discord-message-channel
                                                    channel-id
                                                    :content message
                                                    :stream {:content file-is :filename filename})
                               @(dm/create-message! discord-message-channel
                                                    channel-id
                                                    :content message)))))

(defn create-reaction!
  "A version of discljord.message/create-reaction! that throws errors."
  [discord-message-channel channel-id message-id reaction]
  (check-response-and-throw @(dm/create-reaction! discord-message-channel channel-id message-id reaction)))
