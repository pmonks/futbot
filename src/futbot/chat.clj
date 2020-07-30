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
            [futbot.util           :as u]
            [futbot.config         :as cfg]
            [futbot.ist            :as ist]))

(def prefix "!")

(defn send-message!
  [channel-id message]
  (log/debug "Sending message to channel-id" channel-id ":" message)
  (dm/create-message! cfg/discord-message-channel
                      channel-id
                      :content message))

(defn ist-command!
  "Generates a fake IST video title"
  [_ event-data]
  (send-message! (:channel-id event-data)
                 (str "<:ist:733173880403001394>: \"" (ist/gen-title) "\"")))

(declare help-command!)

(defn privacy-command!
  "Provides a link to the futbot privacy policy"
  [_ event-data]
  (send-message! (:channel-id event-data)
                 "Privacy policy: <https://github.com/pmonks/futbot/blob/main/PRIVACY.md>"))

(defn status-command!
  "Provides technical status of futbot"
  [_ event-data]
  (let [now (tm/instant (tm/with-clock (tm/system-clock "UTC") (tm/zoned-date-time)))]
    (send-message! (:channel-id event-data)
                   (str "futbot running on Clojure " (clojure-version) " / JVM " (System/getProperty "java.version") " (" (System/getProperty "os.arch") ")"
                        "\nBuilt at " (tm/format :iso-instant cfg/built-at) (if cfg/git-url (str " from <" cfg/git-url ">") "")
                        "\nRunning for " (u/human-readable-date-diff cfg/boot-time now)))))

(defn gc-command!
  "Requests that the JVM perform a GC cycle."
  [_ event-data]
  (System/gc)
  (send-message! (:channel-id event-data)
                 "Garbage collection requested."))

; Table of "public" commands; those that can be used in any channel, group or DM
(def public-command-dispatch-table
  {"ist" #'ist-command!})

; Table of "private" commands; those that can only be used in a DM channel
(def private-command-dispatch-table
  {"help"    #'help-command!
   "privacy" #'privacy-command!})

(def secret-command-dispatch-table
  {"status"  #'status-command!
   "gc"      #'gc-command!})

(defn help-command!
  "Displays this help message"
  [_ event-data]
  (send-message! (:channel-id event-data)
                 (str "I understand the following commands in any channel:\n"
                      (s/join "\n" (map #(str " • **`" prefix (key %) "`** - " (:doc (meta (val %))))
                                        (sort-by key public-command-dispatch-table)))
                      "\nAnd the following commands in a DM channel:\n"
                      (s/join "\n" (map #(str " • **`" prefix (key %) "`** - " (:doc (meta (val %))))
                                        (sort-by key private-command-dispatch-table))))))

; Responsive fns
(defmulti handle-discord-event
  "Discord event handler"
  (fn [event-type event-data]
    event-type))

(defmethod handle-discord-event :message-create
  [event-type event-data]
  ; Only respond to messages sent from a human
  (if (not (:bot (:author event-data)))
    (future    ; Spin off the actual processing, so we don't clog the Discord event queue
      (try
        (let [content (s/triml (:content event-data))]
          (if (s/starts-with? content prefix)
            ; Parse the requested command and call it, if it exists
            (let [command-and-args  (s/split content #"\s+" 2)
                  command           (s/lower-case (subs (s/trim (first command-and-args)) (count prefix)))
                  args              (second command-and-args)]
              (if-let [public-command-fn (get public-command-dispatch-table command)]
                (do
                  (log/debug (str "Calling public command fn for '" command "' with args '" args "'."))
                  (public-command-fn args event-data))
                (if-not (:guild-id event-data)
                  (if-let [private-command-fn (get private-command-dispatch-table command)]
                    (do
                      (log/debug (str "Calling private command fn for '" command "' with args '" args "'."))
                      (private-command-fn args event-data))
                    (if-let [secret-command-fn (get secret-command-dispatch-table command)]
                      (do
                        (log/debug (str "Calling secret command fn for '" command "' with args '" args "'."))
                        (secret-command-fn args event-data))
                      (help-command! nil event-data))))))   ; If the requested private command doesn't exist, provide help
            ; If any unrecognised message was sent to a DM channel, provide help
            (if-not (:guild-id event-data)
              (help-command! nil event-data))))
        (catch Exception e
          (log/error e))))))

; Default Discord event handler (noop)
(defmethod handle-discord-event :default
  [event-type event-data])
