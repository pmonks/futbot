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

(ns futbot.posts
  (:require [clojure.string        :as s]
            [clojure.tools.logging :as log]
            [java-time             :as tm]
            [futbot.message-util   :as mu]
            [futbot.source.pro     :as pro]))

(defn check-for-new-pro-insights-and-post!
  "Checks whether amy new PRO Insights have been posted in the last time-period-hours hours (defaults to 24), and posts them to the given channel if so."
  ([config] (check-for-new-pro-insights-and-post! config 24))
  ([{:keys [discord-message-channel post-channel-id education-and-resources-channel-id]}
    time-period-hours]
   (let [new-insights (pro/insights (tm/minus (tm/instant) (tm/hours time-period-hours)))]
     (log/info (str (count new-insights) " new PRO Insight(s) found"))
     (doall (map #(mu/create-message! discord-message-channel
                                      post-channel-id
                                      :content (str "<:pro:778688391608926278> A new **PRO Insight** has been posted, on the topic of **"
                                                    (s/replace (:title %) "PRO Insight: " "")
                                                    "**: "
                                                    (:link %)
                                                    "\nDiscuss in " (mu/channel-link education-and-resources-channel-id) "!"))
                 new-insights)))))
