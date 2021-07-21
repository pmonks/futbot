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

(ns futbot.quizzes
  (:require [clojure.tools.logging            :as log]
            [java-time                        :as tm]
            [futbot.message-util              :as mu]
            [futbot.source.dutch-referee-blog :as drb]
            [futbot.source.cnra               :as cnra]))

(defn check-for-new-dutch-referee-blog-quiz-and-post!
  "Checks whether a new Dutch referee blog quiz has been posted in the last time-period-hours hours (defaults to 24), and posts it to the given channel if so."
  ([config] (check-for-new-dutch-referee-blog-quiz-and-post! config 24))
  ([{:keys [discord-message-channel education-and-resources-channel-id quiz-channel-id]}
    time-period-hours]
   (if-let [new-quizzes (drb/quizzes (tm/minus (tm/instant) (tm/hours time-period-hours)))]
     (let [_          (log/info (str (count new-quizzes) " new Dutch referee blog quizz(es) found"))
           message    (str "<:dfb:753779768306040863> A new **Dutch Referee Blog Laws of the Game Quiz** has been posted: "
                           (:link (first new-quizzes))
                           "\nPuzzled by an answer? Click the react and we'll discuss in " (mu/channel-link education-and-resources-channel-id) "!")
           message-id (:id (mu/create-message! discord-message-channel
                                               quiz-channel-id
                                               :content message))]
       (if message-id
         (do
           (mu/create-reaction! discord-message-channel quiz-channel-id message-id "1️⃣")
           (mu/create-reaction! discord-message-channel quiz-channel-id message-id "2️⃣")
           (mu/create-reaction! discord-message-channel quiz-channel-id message-id "3️⃣")
           (mu/create-reaction! discord-message-channel quiz-channel-id message-id "4️⃣")
           (mu/create-reaction! discord-message-channel quiz-channel-id message-id "5️⃣"))
         (log/warn "No message id found for Dutch referee blog message - skipped adding reactions"))
       nil)
     (log/info "No new Dutch referee blog quizzes found"))))

(defn check-for-new-cnra-quiz-and-post!
  "Checks whether any new CNRA quizzes has been posted in the last month, and posts them to the given channel if so."
  [{:keys [discord-message-channel quiz-channel-id education-and-resources-channel-id]}]
  (if-let [new-quizzes (cnra/quizzes (tm/minus (tm/local-date) (tm/months 1)))]
    (doall
      (map (fn [quiz]
        (let [message (str "<:cnra:769311341751959562> The **"
                           (:quiz-date quiz)
                           " CNRA Quiz** has been posted, on the topic of **"
                           (:topic quiz)
                           "**: "
                           (:link quiz)
                           "\nPuzzled by an answer? React and we'll discuss in " education-and-resources-channel-id ".")]
          (mu/create-message! discord-message-channel
                              quiz-channel-id
                              :content message)))
         new-quizzes))
    (log/info "No new CNRA quizzes found")))
