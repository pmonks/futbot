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
            [clojure.instant       :as inst]
            [clojure.tools.logging :as log]
            [java-time             :as tm]
            [discljord.formatting  :as df]
            [discljord.messaging   :as dm]
            [futbot.util           :as u]
            [futbot.message-util   :as mu]
            [futbot.config         :as cfg]
            [futbot.source.ist     :as ist]
            [futbot.blocklist      :as blk]))

(def prefix "!")

(defn ist-command!
  "Generates a fake IST video title"
  [_ event-data]
  (let [channel-id (:channel-id event-data)]
    (if (or (mu/direct-message? event-data)                       ; Only respond if message was sent via DM or
            (some #{channel-id} (:ist-channel-ids cfg/config)))   ; one of the allowed IST channels.
      (mu/create-message! (:discord-message-channel cfg/config)
                          channel-id
                          :embed (assoc (mu/embed-template)
                                          :description (str "**" (ist/gen-title) "**")
                                          :footer      {:text     "Disclaimer: this is a generated fake"
                                                        :icon_url "https://yt3.ggpht.com/ytc/AAUvwnjhzwc9yNfyfX8C1N820yMhaS27baWlSz2wqaRE=s176-c-k-c0x00ffffff-no-rj"}))
      (log/info (str "Ignoring " prefix "ist command in channel " channel-id)))))

(defn move-command!
  "Moves a conversation to the specified channel"
  [args event-data]
  (when (not (mu/direct-message? event-data))   ; Only respond if the message was sent to a real channel in a server (i.e. not in a DM)
    (let [guild-id                (:guild-id event-data)
          channel-id              (:channel-id event-data)
          discord-message-channel (:discord-message-channel cfg/config)]
      (if (not (s/blank? args))
        (if-let [target-channel-id (second (re-find df/channel-mention args))]
          (if (not= channel-id target-channel-id)
            (let [move-message-id    (:id event-data)
                  _                  (mu/delete-message! discord-message-channel channel-id move-message-id)   ; Don't delete the original message unless we've validated everything
                  target-message-id  (:id (mu/create-message! discord-message-channel
                                                              target-channel-id
                                                              :embed (assoc (mu/embed-template)
                                                                            :description (str "Continuing the conversation from " (mu/channel-link channel-id) "..."))))
                  target-message-url (mu/message-url guild-id target-channel-id target-message-id)
                  source-message-id  (:id (mu/create-message! discord-message-channel
                                                              channel-id
                                                              :embed (assoc (mu/embed-template)
                                                                            :description (str "Let's continue this conversation in " (mu/channel-link target-channel-id) " ([link](" target-message-url "))."))))
                  source-message-url (mu/message-url guild-id channel-id source-message-id)]
              (mu/edit-message! discord-message-channel
                                target-channel-id
                                target-message-id
                                :embed (assoc (mu/embed-template)
                                              :description (str "Continuing the conversation from " (mu/channel-link channel-id)  " ([link](" source-message-url "))..."))))
            (log/info "Cannot move a conversation to the same channel."))
          (log/warn "Could not find target channel in move command."))
        (log/warn "move-command! arguments missing a target channel.")))))

(defn epoch-command!
  "Displays the 'epoch seconds' value of the given date (in RFC-3339 format), or now if no value is provided."
  [args event-data]
  (let [channel-id (:channel-id event-data)]
    (try
      (let [d     (if (s/blank? args) (java.util.Date.) (inst/read-instant-date args))
            epoch (long (/ (.getTime ^java.util.Date d) 1000))]
        (mu/create-message! (:discord-message-channel cfg/config)
                            channel-id
                            :embed (assoc (mu/embed-template) :description (str "`" epoch "`"))))
      (catch RuntimeException re
        (mu/create-message! (:discord-message-channel cfg/config)
                            channel-id
                            :embed (assoc (mu/embed-template) :description (.getMessage re)))))))

(defn dmath-command!
  "Displays the result of the given date math expression e.g. now + 1 day"
  [args event-data]
  (let [channel-id (:channel-id event-data)]
    (try
      (let [[b o v u]  (s/split (s/lower-case (s/trim args)) #"\s+")
            base       (if (= b "now") (.getEpochSecond (tm/instant)) (u/parse-int b))
            op         (case o
                         "-" -
                         "+" +
                         nil)
            val        (u/parse-int v)
            multiplier (case u
                         ("m" "min" "mins" "minutes") 60
                         ("h" "hr" "hrs" "hours")     (* 60 60)
                         ("d" "day" "days")           (* 60 60 24)
                         ("w" "wk" "wks" "weeks")     (* 60 60 24 7)
                         1)]  ; Default to seconds
        (if base
          (if (and op val multiplier)  ; Everything was provided - evaluate the expression
            (mu/create-message! (:discord-message-channel cfg/config)
                                channel-id
                                :embed (assoc (mu/embed-template) :description (str "`" (op base (* val multiplier)) "`")))
            (if-not (or op val)  ; Only base was provided - display it
              (mu/create-message! (:discord-message-channel cfg/config)
                                  channel-id
                                  :embed (assoc (mu/embed-template) :description (str "`" base "`")))
              (throw (ex-info "Op, val or multiplier not provided" {}))))
          (throw (ex-info "Base not provided" {}))))
      (catch Exception _
        (mu/create-message! (:discord-message-channel cfg/config)
                            channel-id
                            :embed (assoc (mu/embed-template) :description (str "Unable to parse date math expression: `" args "`")))))))

(defn privacy-command!
  "Provides a link to the futbot privacy policy"
  [_ event-data]
  (mu/create-message! (:discord-message-channel cfg/config)
                      (:channel-id event-data)
                      :embed (assoc (mu/embed-template)
                                    :description "[futbot's privacy policy is available here](https://github.com/pmonks/futbot/blob/main/PRIVACY.md).")))

(defn status-command!
  "Provides technical status of futbot"
  [_ event-data]
  (let [now (tm/instant)]
    (mu/create-message! (:discord-message-channel cfg/config)
                        (:channel-id event-data)
                        :embed (assoc (mu/embed-template)
                                      :title "futbot Status"
                                      :fields [
                                        {:name "Running for"            :value (str (u/human-readable-date-diff cfg/boot-time now))}
                                        {:name "Built at"               :value (str (tm/format :iso-instant cfg/built-at) (if cfg/git-url (str " from [" cfg/git-tag "](" cfg/git-url ")") ""))}

                                        ; Table of fields here
                                        {:name "Clojure"                :value (str "v" (clojure-version)) :inline true}
                                        {:name "JVM"                    :value (str (System/getProperty "java.vm.vendor") " v" (System/getProperty "java.vm.version") " (" (System/getProperty "os.name") "/" (System/getProperty "os.arch") ")") :inline true}
                                        ; Force a newline (Discord is hardcoded to show 3 fields per line), by using Unicode zero width spaces (empty/blank strings won't work!)
                                        {:name "​"               :value "​" :inline true}
                                        {:name "Heap memory in use"     :value (u/human-readable-size (.getUsed (.getHeapMemoryUsage (java.lang.management.ManagementFactory/getMemoryMXBean)))) :inline true}
                                        {:name "Non-heap memory in use" :value (u/human-readable-size (.getUsed (.getNonHeapMemoryUsage (java.lang.management.ManagementFactory/getMemoryMXBean)))) :inline true}
                                      ]))))

(defn gc-command!
  "Requests that the JVM perform a GC cycle."
  [_ event-data]
  (System/gc)
  (mu/create-message! (:discord-message-channel cfg/config)
                      (:channel-id event-data)
                      :content "Garbage collection requested."))

(defn set-logging-command!
  "Sets the log level, optionally for the given logger (defaults to 'futbot')."
  [args event-data]
  (let [[level logger] (s/split args #"\s+")]
    (if level
      (do
        (cfg/set-log-level! level (if logger logger "futbot"))
        (mu/create-message! (:discord-message-channel cfg/config)
                            (:channel-id event-data)
                            :content (str "Logging level " (s/upper-case level) " set" (if logger (str " for logger '" logger "'") " for logger 'futbot'") ".")))
      (mu/create-message! (:discord-message-channel cfg/config)
                          (:channel-id event-data)
                          :content "Logging level not provided; must be one of: ERROR, WARN, INFO, DEBUG, TRACE"))))

(defn debug-logging-command!
  "Enables debug logging, which turns on TRACE for 'discljord' and DEBUG for 'futbot'."
  [_ event-data]
  (cfg/set-log-level! "TRACE" "discljord")
  (cfg/set-log-level! "DEBUG" "futbot")
  (mu/create-message! (:discord-message-channel cfg/config)
                      (:channel-id event-data)
                      :content "Debug logging enabled (TRACE for 'discljord' and DEBUG for 'futbot')."))

(defn reset-logging-command!
  "Resets all log levels to their configured defaults."
  [_ event-data]
  (cfg/reset-logging!)
  (mu/create-message! (:discord-message-channel cfg/config)
                      (:channel-id event-data)
                      :content "Logging configuration reset."))


; Table of "public" commands; those that can be used in any channel, group or DM
(def global-command-dispatch-table
  {"move"  #'move-command!
   "epoch" #'epoch-command!
   "dmath" #'dmath-command!})

(def memes-command-dispatch-table
  {"ist" #'ist-command!})

(def public-command-dispatch-table
  (into global-command-dispatch-table memes-command-dispatch-table))

(declare help-command!)

; Table of "private" commands; those that can only be used in a DM channel
(def private-command-dispatch-table
  {"help"    #'help-command!
   "privacy" #'privacy-command!})

(def secret-command-dispatch-table
  {"status"       #'status-command!
   "gc"           #'gc-command!
   "setlogging"   #'set-logging-command!
   "debuglogging" #'debug-logging-command!
   "resetlogging" #'reset-logging-command!})

(defn help-command!
  "Displays this help message"
  [_ event-data]
  (mu/create-message! (:discord-message-channel cfg/config)
                      (:channel-id event-data)
                      :embed (assoc (mu/embed-template)
                                    :description (str "I understand the following command(s) in any channel or DM:\n"
                                                      (s/join "\n" (map #(str " • **`" prefix (key %) "`** - " (:doc (meta (val %))))
                                                                        (sort-by key global-command-dispatch-table)))
                                                      "\n\nAnd the following command(s) in " (mu/channel-link "683853455038742610") " or a DM:\n"
                                                      (s/join "\n" (map #(str " • **`" prefix (key %) "`** - " (:doc (meta (val %))))
                                                                        (sort-by key memes-command-dispatch-table)))
                                                      "\n\nAnd the following command(s) only in a DM:\n"
                                                      (s/join "\n" (map #(str " • **`" prefix (key %) "`** - " (:doc (meta (val %))))
                                                                        (sort-by key private-command-dispatch-table)))))))

; Responsive fns
(defmulti handle-discord-event
  "Discord event handler"
  (fn [event-type _] event-type))

; Default Discord event handler (noop)
(defmethod handle-discord-event :default
  [_ _])

(defmethod handle-discord-event :message-create
  [_ event-data]
  ; Only respond to messages sent from a human
  (when (mu/human-message? event-data)
    (future    ; Spin off the actual processing, so we don't clog the Discord event queue
      (try
        (when-not (blk/check-blocklist! event-data)  ; First check if the given message violates the blocklist
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
                  (when (mu/direct-message? event-data)
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
              (when-not (:guild-id event-data)
                (help-command! nil event-data)))))
        (catch Exception e
          (u/log-exception e))))))

(defmethod handle-discord-event :message-update
  [_ event-data]
  ; Only respond to messages sent from a human
  (when (mu/human-message? event-data)
    (future    ; Spin off the actual processing, so we don't clog the Discord event queue
      (try
        (blk/check-blocklist! event-data)       ; Check if the updated message violates the blocklist
        (when (bit-test (:flags event-data) 5)  ; 5 = HAS_THREAD - see https://discord.com/developers/docs/resources/channel#message-object-message-flags
          (let [thread-id (:id event-data)]
            (when-not @(dm/join-thread! (:discord-connection-channel cfg/config) thread-id)
              (log/warn "Failed to join thread '" thread-id "'."))))
        (catch Exception e
          (u/log-exception e))))))
