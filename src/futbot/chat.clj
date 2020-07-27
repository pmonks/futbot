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

(ns futbot.chat
  (:require [clojure.string        :as s]
            [clojure.java.io       :as io]
            [clojure.tools.logging :as log]
            [java-time             :as tm]
            [discljord.connections :as dc]
            [discljord.messaging   :as dm]
            [discljord.events      :as de]
            [futbot.config         :as cfg]
            [futbot.ist            :as ist]))

(def prefix "!")

(defn ist-command
  [_ event-data]
  (dm/create-message! cfg/discord-message-channel
                      (:channel-id event-data)
                      :content (str "<:ist:733173880403001394>: \"" (ist/gen-title) "\"")))

(def command-dispatch-table
  {"ist" ist-command})

; Responsive fns
(defmulti handle-discord-event
  "Discord event handler"
  (fn [event-type event-data]
    event-type))

(defmethod handle-discord-event :message-create
  [event-type event-data]
  (try
    (let [content (s/triml (:content event-data))]
      (if (s/starts-with? content prefix)
        (let [command-and-args (s/split content #"\s+" 2)
              command          (s/lower-case (subs (s/trim (first command-and-args)) (count prefix)))
              args             (second command-and-args)
              command-fn       (get command-dispatch-table command)]
          (if command-fn
            (future  ; Fire off the command handler asynchronously
              (log/debug (str "Calling command fn for '" command "' with args '" args "'."))
              command-fn args event-data)))))
    (catch Exception e
      (log/error e))))

; Default Discord event handler (noop)
(defmethod handle-discord-event :default
  [event-type event-data])
