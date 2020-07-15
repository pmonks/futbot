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

(ns futbot.ist.main
  (:require [clojure.string     :as s]
            [org.httpkit.client :as http]
            [cheshire.core      :as ch]
            [markov-chains.core :as mc]
            [futbot.util        :as u])
  (:gen-class))

(def ist-youtube-channel-id "UCmzFaEBQlLmMTWS0IQ90tgA")

(def api-host                       "https://www.googleapis.com")
(def endpoint-youtube-search-page-1 "/youtube/v3/search?part=snippet&type=video&maxResults=50&order=date&key=%s&channelId=%s")
(def endpoint-youtube-search-page-n (str endpoint-youtube-search-page-1 "&pageToken=%s"))

(defn all-videos-for-channel
  "Retrieves all videos for the given YouTube channel id, by paging through 50 at a time."
  [google-api-key channel-id]
  (loop [api-url (format (str api-host endpoint-youtube-search-page-1) google-api-key channel-id)
         result  (atom [])]
    (let [{:keys [status headers body error]} @(http/get api-url)
          api-response                        (ch/parse-string body u/clojurise-json-key)
          next-page                           (:next-page-token api-response)]
      (swap! result concat (:items api-response))
      (if (not (s/blank? next-page))
        (recur (format (str api-host endpoint-youtube-search-page-n) google-api-key channel-id next-page)
               result)
        @result))))

(defn exit
  [msg code]
  (binding [*out* *err*]
    (println msg)
    (flush))
  (System/exit code))

(defn -main
  [& args]
  (try
    (if (not= 1 (count args))
      (exit "Please provide a Google API key on the command line." -1))

    (let [google-api-key (first args)
          ist-videos     (all-videos-for-channel google-api-key ist-youtube-channel-id)
          ist-titles     (map #(:title (:snippet %)) ist-videos)
          ist-titles-str (u/replace-all (s/join " 🔚 " ist-titles)
                                        [["&amp;"             "&"]
                                         ["&quot;"            "\""]
                                         ["&#39;"             "'"]
                                         ["’"                 "'"]
                                         [#"[“”]"             "\""]
                                         [#"([!?:;,\"…\*])"   " $1 "]
                                         [#"((?<!\s))(\.+)\s" "$1 $2 "]
                                         ])
          words          (s/split ist-titles-str #"\s+")
          chain          (mc/collate words 2)]
      (spit "resources/ist-markov-chain.edn" (pr-str chain)))
    (catch Exception e
      (exit e -1))))
