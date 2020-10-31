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

(ns futbot.source.youtube
  (:require [clojure.tools.logging :as log]
            [org.httpkit.client    :as http]
            [cheshire.core         :as ch]
            [java-time             :as tm]
            [futbot.util           :as u]))

(def api-host                    "https://www.googleapis.com")
(def endpoint-get-channel-info   "/youtube/v3/channels?part=snippet&id=%s")
(def endpoint-get-channel-videos "/youtube/v3/search?part=snippet&order=date&type=video&maxResults=50&channelId=%s")

(defn google-api-call
  "Calls the given Google API endpoint (must be fully constructed), using the provided API key, and either returns parsed hashmap of the body or throws an ex-info."
  [youtube-api-token endpoint]
  (let [api-url                     (str api-host endpoint)
        options                     {:headers {"Accept" "application/json"}}
        _                           (log/debug "Calling" (str api-url "&key=REDACTED"))
        {:keys [status body error]} @(http/get (str api-url "&key=" youtube-api-token) options)]
    (if (or error (not= status 200))
      (throw (ex-info (format "Google API call (%s) failed" (str api-url "&key=REDACTED")) {:status status :body (ch/parse-string body u/clojurise-json-key)} error))
      (ch/parse-string body u/clojurise-json-key))))

(defn channel-info
  "Returns info for the given channel, or throws an ex-info."
  [youtube-api-token channel-id]
  (:snippet (first (:items (google-api-call youtube-api-token (format endpoint-get-channel-info channel-id))))))

(defn videos
  "Retrieves up to 50 videos for the given YouTube channel (or nil if there aren't any), optionally limited to those published since the given date."
  ([youtube-api-token channel-id] (videos youtube-api-token nil channel-id))
  ([youtube-api-token since channel-id]
    (let [endpoint (if since
                     (format (str endpoint-get-channel-videos "&publishedAfter=%s") channel-id (str (tm/instant since)))
                     (format endpoint-get-channel-videos channel-id))]
      (seq (map #(assoc (:snippet %) :id (:video-id (:id %)))              ; Herpa derp Youtube API 🙄
                (:items (google-api-call youtube-api-token endpoint)))))))
