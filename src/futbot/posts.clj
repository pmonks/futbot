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
  (:require [clojure.tools.logging       :as log]
            [java-time                   :as tm]
            [futbot.discord.message-util :as mu]
            [futbot.source.pro           :as pro]))

(defn- post-pro-post!
  [{:keys [discord-message-channel post-channel-id pro-category-to-channel-map education-and-resources-channel-id]}
   post]
  (let [categories (map :category post)
        channel-id (if-let [first-channel-id (first (filter identity (map (partial get pro-category-to-channel-map) categories)))]
                     first-channel-id
                     post-channel-id)]
    (mu/create-message! discord-message-channel
                        channel-id
                        :content (str "<:pro:778688391608926278> A new **PRO article** has been posted: **"
                                      (:title post)
                                      "**: "
                                      (:link post)
                                      (when (= channel-id post-channel-id ) (str "\nDiscuss in " (mu/channel-link education-and-resources-channel-id) "."))))))

(defn check-for-new-pro-posts-and-post!
  "Checks whether amy new PRO Insights have been posted in the last time-period-hours hours (defaults to 24), and posts them to the given channel if so."
  ([config] (check-for-new-pro-posts-and-post! config 24))
  ([config time-period-hours]
   (let [new-posts (pro/posts-of-interest-since (tm/minus (tm/instant) (tm/hours time-period-hours)))]
     (log/info (str (count new-posts) " new PRO post(s) found"))
     (doall (map (partial post-pro-post! config) new-posts)))))
