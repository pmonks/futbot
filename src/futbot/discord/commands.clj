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
            [mount.core                  :as mnt :refer [defstate]]
            [discljord.messaging         :as dm]
            [slash.core                  :as sc]
            [slash.command               :as scmd :refer [defhandler]]
            [slash.command.structure     :as scs]
            [slash.gateway               :as sg]
            [futbot.util                 :as u]
            [futbot.config               :as cfg]
            [futbot.discord.message-util :as mu]
            [futbot.discord.routing      :as rt]))

(defn- rationalise-options-map
  "Takes an interaction map and assocs the user-supplied :args in a more idiomatic format."
  [interaction]
  (if-let [option-map (get-in interaction [:data :option-map])]
    (assoc interaction :args (apply hash-map (mapcat identity option-map)))
    interaction))

; /futpoll
(def futpoll-command
  (scs/command
   "futpoll"
   "Creates a poll"
   :options
   [(scs/option "incident-clip" "A link to the video clip showing the incident" :string  :required true)
    (scs/option "poll-types"    "The type(s) of poll to post"                   :string  :required false
                :choices [(scs/choice "Sanction (default)" "sanction")
                          (scs/choice "Restart"            "restart")
                          (scs/choice "Offside"            "offside")
                          (scs/choice "Sanction & Restart" "sanction,restart")])]))

(defhandler futpoll-handler
  ["futpoll"] ; Command path
  {:keys [id token data] :as _interaction}
  _
  (let [interaction   (rationalise-options-map _interaction)
        incident-clip (get-in interaction [:args :incident-clip])]
    (if (u/is-url? incident-clip)
      (let [poll-types (get-in interaction [:args :poll-types] "sanction")
            channel-id (:channel-id interaction)]
        ; Incident clip message
        (mu/respond-to-interaction! (:discord-message-channel cfg/config)
                                    id token 4 ; 4 == channel message
                                    :data  {:content incident-clip})   ; Put the incident clip in the content, so that the Discord client creates a preview for it
        ; Sanction poll message
        (when (s/includes? poll-types "sanction")
          (let [message-id (:id (mu/create-message! (:discord-message-channel cfg/config)
                                                    channel-id
                                                    :embed (assoc (mu/embed-template-no-footer)
                                                                  :description (str "❌ No foul\n"
                                                                                    "✅ Foul (no sanction)\n"
                                                                                    "<:YC:698349911028269078> Foul + caution\n"
                                                                                    "<:RC:698350061922418738> Foul + send-off\n"
                                                                                    "<:dive:808120608301908039> Simulation"))))]
           (mu/create-reaction! (:discord-message-channel cfg/config) channel-id message-id "❌")
           (mu/create-reaction! (:discord-message-channel cfg/config) channel-id message-id "✅")
           (mu/create-reaction! (:discord-message-channel cfg/config) channel-id message-id "YC:698349911028269078")
           (mu/create-reaction! (:discord-message-channel cfg/config) channel-id message-id "RC:698350061922418738")
           (mu/create-reaction! (:discord-message-channel cfg/config) channel-id message-id "dive:808120608301908039")))
        ; Restart poll message
        (when (s/includes? poll-types "restart")
          (let [message-id (:id (mu/create-message! (:discord-message-channel cfg/config)
                                                    channel-id
                                                    :embed (assoc (mu/embed-template-no-footer)
                                                                  :description (str "<:whistle:753061925231001640> Kickoff\n"
                                                                                    "🙌 Throw-in\n"
                                                                                    "✋ Indirect free kick\n"
                                                                                    "👉 Direct free kick\n"
                                                                                    "⚪ Penalty kick\n"
                                                                                    "⚽ Drop ball\n"
                                                                                    "🥅 Goal kick\n"
                                                                                    "🚩 Corner kick"))))]
           (mu/create-reaction! (:discord-message-channel cfg/config) channel-id message-id "whistle:753061925231001640")
           (mu/create-reaction! (:discord-message-channel cfg/config) channel-id message-id "🙌")
           (mu/create-reaction! (:discord-message-channel cfg/config) channel-id message-id "✋")
           (mu/create-reaction! (:discord-message-channel cfg/config) channel-id message-id "👉")
           (mu/create-reaction! (:discord-message-channel cfg/config) channel-id message-id "⚪")   ; BE VERY CAREFUL HERE - the default ⚪️ emoji from macOS is not supported as a react in Discord!
           (mu/create-reaction! (:discord-message-channel cfg/config) channel-id message-id "⚽")   ; BE VERY CAREFUL HERE - the default ⚽️ emoji from macOS is not supported as a react in Discord!
           (mu/create-reaction! (:discord-message-channel cfg/config) channel-id message-id "🥅")
           (mu/create-reaction! (:discord-message-channel cfg/config) channel-id message-id "🚩")))
        ; Offside poll message
        (when (s/includes? poll-types "offside")
          (let [message-id (:id (mu/create-message! (:discord-message-channel cfg/config)
                                                    channel-id
                                                    :embed (assoc (mu/embed-template-no-footer)
                                                                  :description (str "❌ Not offside\n"
                                                                                    "<:ar:753060321601781819> Offside\n"
                                                                                    "⚽ Interfering with play\n"
                                                                                    "🏃 Interfering with an opponent\n"
                                                                                    "🥅 Gaining an advantage"))))]
           (mu/create-reaction! (:discord-message-channel cfg/config) channel-id message-id "❌")
           (mu/create-reaction! (:discord-message-channel cfg/config) channel-id message-id "ar:753060321601781819")
           (mu/create-reaction! (:discord-message-channel cfg/config) channel-id message-id "⚽")
           (mu/create-reaction! (:discord-message-channel cfg/config) channel-id message-id "🏃")
           (mu/create-reaction! (:discord-message-channel cfg/config) channel-id message-id "🥅"))))
        ; Invalid link - warn the user with an ephemeral message
        (mu/respond-to-interaction! (:discord-message-channel cfg/config)
                                    id token 4 ; 4 == channel message
                                    :data  {:flags  64   ; 64 == ephemeral message
                                            :embeds [{:description (str "'" incident-clip "' is not a valid link. Please check it and try again." )}]}))))

; Routing
(def interaction-handlers
  (assoc sg/gateway-defaults
         :application-command (scmd/paths #'futpoll-handler)))

(defmethod rt/handle-discord-event :interaction-create
  [_ event-data]
  (sc/route-interaction interaction-handlers event-data))

; Eegisters the slash command(s) with Discord
(declare commands)
(defstate commands
  :start @(dm/bulk-overwrite-guild-application-commands! (:discord-message-channel cfg/config)
                                                         (:discord-application-id  cfg/config)
                                                         (:guild-id                cfg/config)
                                                         [futpoll-command]))
