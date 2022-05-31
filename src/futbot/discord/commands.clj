;
; Copyright © 2022 Peter Monks
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

(ns futbot.discord.commands
  (:require [clojure.string              :as s]
            [clojure.instant             :as inst]
            [java-time                   :as tm]
            [mount.core                  :as mnt :refer [defstate]]
            [slash.core                  :as sc]
            [slash.command               :as scmd :refer [defhandler]]
            [slash.command.structure     :as scs]
            [slash.gateway               :as sg]
            [futbot.util                 :as u]
            [futbot.config               :as cfg]
            [futbot.discord.message-util :as mu]
            [futbot.discord.routing      :as rt]
            [futbot.source.ist           :as ist]))

(def ^:private channel-message          4)
(def ^:private deferred-channel-message 5)
(def ^:private ephemeral                64)

(defn- rationalise-options-map
  "Takes an interaction map and assocs the user-supplied :args in a more idiomatic format."
  [interaction]
  (if-let [option-map (get-in interaction [:data :option-map])]
    (assoc interaction :args (apply hash-map (mapcat identity option-map)))
    interaction))

; /ist
(def ist-command
  (scs/command
   "ist"
   "Generates a fake IST video title"
   :options []))

(declare ist-handler)  ; To shush clj-kondo
(defhandler ist-handler
  ["ist"]
  {:keys [id token data] :as _interaction}
  _
  (let [discord-message-channel (:discord-message-channel cfg/config)
        interaction             (rationalise-options-map _interaction)
        channel-id              (:channel-id interaction)]
    (if (some #{channel-id} (:ist-channel-ids cfg/config))   ; Only respond if /ist was used in one of the allowed IST channels.
      (mu/create-interaction-response! discord-message-channel id token channel-message
                                       :data {:embeds [(assoc (mu/embed-template)
                                                              :description (str "**" (ist/gen-title) "**")
                                                              :footer      {:text     "Disclaimer: this is a generated fake"
                                                                            :icon_url "https://yt3.ggpht.com/ytc/AAUvwnjhzwc9yNfyfX8C1N820yMhaS27baWlSz2wqaRE=s176-c-k-c0x00ffffff-no-rj"})]})
      (mu/create-interaction-response! discord-message-channel id token channel-message
                                       :data {:flags  ephemeral
                                              :embeds [(assoc (mu/embed-template) :description "`/ist` is not allowed here <:disappointed_toledo:861672902691782674>")]}))))

; /move
(def move-command
  (scs/command
   "move"
   "Moves a conversation to another channel"
   :options [(scs/option "target-channel" "The channel to move the conversation to" :channel :required true)]))

(declare move-handler)  ; To shush clj-kondo
(defhandler move-handler
  ["move"]
  {:keys [id token data] :as _interaction}
  _
  (let [discord-message-channel (:discord-message-channel cfg/config)
        interaction             (rationalise-options-map _interaction)
        guild-id                (:guild-id interaction)
        source-channel-id       (:channel-id interaction)
        target-channel-id       (get-in interaction [:args :target-channel])]
    (if (not= source-channel-id target-channel-id)
      (let [; 1. Acknowledge the interaction without sending a message
            _                  (mu/create-interaction-response! discord-message-channel id token deferred-channel-message :data {:flags ephemeral})
            ; Create the message in the target channel
            target-message-id  (:id (mu/create-message! discord-message-channel target-channel-id
                                                        :embed (assoc (mu/embed-template)
                                                                      :description (str "Continuing the conversation from " (mu/channel-link source-channel-id) "..."))))
            ; 2. Create the message in the source channel
            target-message-url (mu/message-url guild-id target-channel-id target-message-id)
            source-message-id  (:id (mu/create-message! discord-message-channel source-channel-id
                                                        :embed (assoc (mu/embed-template)
                                                                      :description (str "Let's continue this conversation in " (mu/channel-link target-channel-id) " ([link](" target-message-url "))."))))
            ; 3. Edit the message in the target channel to add the link to the message in the source channel (i.e. cross-link the two messages)
            source-message-url (mu/message-url guild-id source-channel-id source-message-id)
            _                  (mu/edit-message! discord-message-channel target-channel-id target-message-id
                                                 :embed (assoc (mu/embed-template)
                                                               :description (str "Continuing the conversation from " (mu/channel-link source-channel-id)  " ([link](" source-message-url "))...")))]
        ; 4. Delete the interaction acknowledgement, so that Discord's idiotic "thinking..." message goes away... ...which doesn't work for some reason...
        (mu/delete-original-interaction-response! discord-message-channel (:discord-application-id cfg/config) token))
      (mu/create-interaction-response! discord-message-channel id token channel-message
                                       :data  {:flags ephemeral
                                               :embeds [(assoc (mu/embed-template) :description "You cannot move a conversation to the same channel.")]}))))

; /epoch
(def epoch-command
  (scs/command
   "epoch"
   "Returns the epoch for a given date (either 'now', or any ISO-8601 formatted date/time string)"
   :options [(scs/option "datetime" "The date/time to convert into the equivalent epoch" :string :required false)]))

(declare epoch-handler)  ; To shush clj-kondo
(defhandler epoch-handler
  ["epoch"]
  {:keys [id token data] :as _interaction}
  _
  (let [discord-message-channel (:discord-message-channel cfg/config)
        interaction             (rationalise-options-map _interaction)
        datetime                (get-in interaction [:args :datetime] "now")]
    (try
      (let [d     (if (or (s/blank? datetime)
                          (= "now" (s/trim (s/lower-case datetime))))
                    (java.util.Date.)
                    (inst/read-instant-date (s/trim datetime)))
            epoch (long (/ (.getTime ^java.util.Date d) 1000))]
        (mu/create-interaction-response! discord-message-channel id token channel-message
                                         :data {:flags  ephemeral
                                                :embeds [(assoc (mu/embed-template) :description (str "`" datetime "` is `" epoch "`"))]}))
      (catch RuntimeException re
        (mu/create-interaction-response! discord-message-channel id token channel-message
                                         :data {:flags  ephemeral
                                                :embeds [(assoc (mu/embed-template) :description (.getMessage re))]})))))

; /dmath
(def dmath-command
  (scs/command
   "dmath"
   "Displays the result of the given date math expression (e.g. 'now + 1 day'), as an epoch value"
   :options [(scs/option "expression" "The date/time math expression to evaluate" :string :required false)]))

(declare dmath-handler)  ; To shush clj-kondo
(defhandler dmath-handler
  ["dmath"]
  {:keys [id token data] :as _interaction}
  _
  (let [discord-message-channel (:discord-message-channel cfg/config)
        interaction             (rationalise-options-map _interaction)
        expression              (s/replace (s/lower-case (s/trim (get-in interaction [:args :expression] "now"))) #"\s+" " ")]
    (try
      (let [[b o v u]  (s/split expression #"\s+")
            base       (if (= b "now") (.getEpochSecond (tm/instant)) (u/parse-int b))
            op         (case o
                         "-" -
                         "+" +
                         nil)
            val        (u/parse-int v)
            multiplier (case u
                         ("s" "sec" "secs" "second" "seconds") 1
                         ("m" "min" "mins" "minute" "minutes") 60
                         ("h" "hr" "hrs" "hour" "hours")       (* 60 60)
                         ("d" "day" "days")                    (* 60 60 24)
                         ("w" "wk" "wks" "week" "weeks")       (* 60 60 24 7)
                         nil)
            message     (cond
                          (and (not (s/blank? b))
                               (not base))        (str "Unable to parse value '" b "'.")
                          (and (not (s/blank? o))
                               (not op))          (str "Unknown operation `" o "`.\n\nfutbot supports `+` and `-`.")
                          (and (not (s/blank? v))
                               (not val))         (str "Unable to parse value '" v "'.")
                          (and (not (s/blank? u))
                               (not multiplier))  (str "Unknown unit '" u "'.\n\nfutbot supports seconds, minutes, hours, days, and weeks (and common abbreviations thereof).")
                          (and op (not val))      (str "No value was provided.")
                          (and op val)            (str "`" expression "` = `" (op base (* val (if multiplier multiplier 1))) "`")   ; Everything was provided - evaluate the expression
                          :else                   (str "`" base "`"))]
        (mu/create-interaction-response! discord-message-channel id token channel-message
                                         :data {:flags  ephemeral
                                                :embeds [(assoc (mu/embed-template) :description message)]}))
      (catch Exception _
        (mu/create-interaction-response! discord-message-channel id token channel-message
                                         :data {:flags  ephemeral
                                                :embeds [(assoc (mu/embed-template) :description (str "Unable to parse date math expression: `" expression "`"))]})))))

; /ts
(def tstag-command
  (scs/command
   "tstag"
   "Creates a timestamp tag"
   :options [(scs/option "timestamp" "The timestamp to use"       :integer :required true)
             (scs/option "style"     "The style of the timestamp" :string  :required false
                         :choices [(scs/choice "Short time (e.g. 16:20)"                              "t")
                                   (scs/choice "Long time (e.g. 16:20:30)"                            "T")
                                   (scs/choice "Short date (e.g. 20/04/2021)"                         "d")
                                   (scs/choice "Long date (e.g. 20 April 2021)"                       "D")
                                   (scs/choice "Short date/time (default) (e.g. 20 April 2021 16:20)" "f")
                                   (scs/choice "Long date/time (e.g. Tuesday, 20 April 2021 16:20)"   "F")
                                   (scs/choice "Relative time (e.g. 2 months ago)"                    "R")])]))

(declare tstag-handler)  ; To shush clj-kondo
(defhandler tstag-handler
  ["tstag"]
  {:keys [id token data] :as _interaction}
  _
  (let [discord-message-channel (:discord-message-channel cfg/config)
        interaction             (rationalise-options-map _interaction)
        timestamp               (get-in interaction [:args :timestamp])
        style                   (get-in interaction [:args :style])]
    (mu/create-interaction-response! discord-message-channel id token channel-message
                                    :data {:flags  ephemeral
                                           :embeds [(assoc (mu/embed-template) :description (str "`<t:" timestamp (when style (str ":" style)) ">`"))]})))

; /futpoll
(def futpoll-command
  (scs/command
   "futpoll"
   "Creates match incident poll(s)"
   :options [(scs/option "incident-clip" "A link to the video clip showing the incident" :string  :required true)
             (scs/option "poll-types"    "The type(s) of poll to post"                   :string  :required false
                         :choices [(scs/choice "Sanction (default)" "sanction")
                                   (scs/choice "Restart"            "restart")
                                   (scs/choice "Offside"            "offside")
                                   (scs/choice "Sanction & Restart" "sanction,restart")])]))

(declare futpoll-handler)  ; To shush clj-kondo
(defhandler futpoll-handler
  ["futpoll"]
  {:keys [id token data] :as _interaction}
  _
  (let [discord-message-channel (:discord-message-channel cfg/config)
        interaction             (rationalise-options-map _interaction)
        incident-clip           (get-in interaction [:args :incident-clip])]
    (if (u/is-url? incident-clip)
      (let [poll-types (get-in interaction [:args :poll-types] "sanction")
            channel-id (:channel-id interaction)]
        ; Incident clip message
        (mu/create-interaction-response! discord-message-channel id token channel-message
                                         :data {:content incident-clip})   ; Put the incident clip in the content, so that the Discord client creates a preview for it - it won't do this if there's an embed
        ; Sanction poll message
        (when (s/includes? poll-types "sanction")
          (let [message-id (:id (mu/create-message! discord-message-channel channel-id
                                                    :embed (assoc (mu/embed-template-no-footer)
                                                                  :description (str "❌ No foul\n"
                                                                                    "✅ Foul (no sanction)\n"
                                                                                    "<:YC:698349911028269078> Foul + caution\n"
                                                                                    "<:RC:698350061922418738> Foul + send-off\n"
                                                                                    "<:dive:808120608301908039> Simulation"))))]
           (mu/create-reaction! discord-message-channel channel-id message-id "❌")
           (mu/create-reaction! discord-message-channel channel-id message-id "✅")
           (mu/create-reaction! discord-message-channel channel-id message-id "YC:698349911028269078")
           (mu/create-reaction! discord-message-channel channel-id message-id "RC:698350061922418738")
           (mu/create-reaction! discord-message-channel channel-id message-id "dive:808120608301908039")))
        ; Restart poll message
        (when (s/includes? poll-types "restart")
          (let [message-id (:id (mu/create-message! discord-message-channel channel-id
                                                    :embed (assoc (mu/embed-template-no-footer)
                                                                  :description (str "<:whistle:753061925231001640> Kickoff\n"
                                                                                    "🙌 Throw-in\n"
                                                                                    "✋ Indirect free kick\n"
                                                                                    "👉 Direct free kick\n"
                                                                                    "⚪ Penalty kick\n"
                                                                                    "⚽ Drop ball\n"
                                                                                    "🥅 Goal kick\n"
                                                                                    "🚩 Corner kick"))))]
           (mu/create-reaction! discord-message-channel channel-id message-id "whistle:753061925231001640")
           (mu/create-reaction! discord-message-channel channel-id message-id "🙌")
           (mu/create-reaction! discord-message-channel channel-id message-id "✋")
           (mu/create-reaction! discord-message-channel channel-id message-id "👉")
           (mu/create-reaction! discord-message-channel channel-id message-id "⚪")   ; BE VERY CAREFUL HERE - the default ⚪️ emoji from macOS is not supported as a react in Discord!
           (mu/create-reaction! discord-message-channel channel-id message-id "⚽")   ; BE VERY CAREFUL HERE - the default ⚽️ emoji from macOS is not supported as a react in Discord!
           (mu/create-reaction! discord-message-channel channel-id message-id "🥅")
           (mu/create-reaction! discord-message-channel channel-id message-id "🚩")))
        ; Offside poll message
        (when (s/includes? poll-types "offside")
          (let [message-id (:id (mu/create-message! discord-message-channel channel-id
                                                    :embed (assoc (mu/embed-template-no-footer)
                                                                  :description (str "❌ Not offside\n"
                                                                                    "<:ar:753060321601781819> Offside\n"
                                                                                    "⚽ Interfering with play\n"
                                                                                    "🏃 Interfering with an opponent\n"
                                                                                    "🥅 Gaining an advantage"))))]
           (mu/create-reaction! discord-message-channel channel-id message-id "❌")
           (mu/create-reaction! discord-message-channel channel-id message-id "ar:753060321601781819")
           (mu/create-reaction! discord-message-channel channel-id message-id "⚽")
           (mu/create-reaction! discord-message-channel channel-id message-id "🏃")
           (mu/create-reaction! discord-message-channel channel-id message-id "🥅"))))
        ; Invalid link - warn the user with an ephemeral message
        (mu/create-interaction-response! discord-message-channel id token channel-message
                                         :data {:flags  ephemeral
                                                :embeds [{:description (str "'" incident-clip "' is not a valid link. Please check it and try again." )}]}))))

; Routing
(def interaction-handlers
  (assoc sg/gateway-defaults
         :application-command (scmd/paths #'ist-handler #'move-handler #'epoch-handler #'dmath-handler #'tstag-handler #'futpoll-handler)))

(defmethod rt/handle-discord-event :interaction-create
  [_ event-data]
  (sc/route-interaction interaction-handlers event-data))

; Eegisters the slash command(s) with Discord
(declare commands)
(defstate commands
  :start (mu/bulk-overwrite-guild-application-commands! (:discord-message-channel cfg/config)
                                                        (:discord-application-id  cfg/config)
                                                        (:guild-id                cfg/config)
                                                        [ist-command move-command epoch-command dmath-command tstag-command futpoll-command]))
