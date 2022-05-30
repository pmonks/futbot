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

(ns futbot.videos
  (:require [clojure.tools.logging       :as log]
            [java-time                   :as tm]
            [futbot.discord.message-util :as mu]
            [futbot.source.youtube       :as yt]))

(def ist-youtube-channel-id "UCmzFaEBQlLmMTWS0IQ90tgA")

(defn post-youtube-video!
  [{:keys [discord-message-channel youtube-channels education-and-resources-channel-id]}
   youtube-channel-id
   video]
  (let [channel-info  (get youtube-channels youtube-channel-id)
        channel-title (org.jsoup.parser.Parser/unescapeEntities (:title channel-info) true)
        message       (str (:emoji channel-info)
                           (if channel-title (str " A new **" channel-title "** video has been posted: **") " A new video has been posted: **")
                           (org.jsoup.parser.Parser/unescapeEntities (:title video) true)
                           "**: https://www.youtube.com/watch?v=" (:id video)
                           (when-not (= youtube-channel-id ist-youtube-channel-id)
                             (str "\nDiscuss in " (mu/channel-link education-and-resources-channel-id) ".")))]
     (mu/create-message! discord-message-channel
                         (:channel-id channel-info)
                         :content message)))

(defn check-for-new-youtube-videos-and-post!
  "Checks whether any new videos have been posted to the given YouTube channel in the last day, and posts it to the given Discord channel if so."
  [{:keys [youtube-api-token youtube-channels] :as config}
   youtube-channel-id]
  (let [channel-title (:title (get youtube-channels youtube-channel-id))]
    (if-let [new-videos (yt/videos youtube-api-token
                                   (tm/minus (tm/instant) (tm/days 1))
                                   youtube-channel-id)]
      (doall (map (partial post-youtube-video! config youtube-channel-id) new-videos))
      (log/info (str "No new videos found in YouTube channel " (if channel-title channel-title (str "-unknown (" youtube-channel-id ")-")))))))
