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
            [clojure.java.io    :as io]
            [clojure.pprint     :as pp]
            [org.httpkit.client :as http]
            [cheshire.core      :as ch]
            [markov-chains.core :as mc]
            [futbot.util        :as u]))

(def ist-youtube-channel-id "UCmzFaEBQlLmMTWS0IQ90tgA")

(def api-host                       "https://www.googleapis.com")
(def endpoint-youtube-search-page-1 "/youtube/v3/search?part=snippet&type=video&maxResults=50&order=date&channelId=%s")
(def endpoint-youtube-search-page-n (str endpoint-youtube-search-page-1 "&pageToken=%s"))

(defn all-videos-for-channel
  "Retrieves all videos for the given YouTube channel id, by paging through 50 at a time."
  [google-api-key channel-id]
  (loop [api-url (format (str api-host endpoint-youtube-search-page-1) channel-id)
         page    1
         result  []]
    (println "Retrieving page" page "of results")
    (let [{:keys [status headers body error]} @(http/get (str api-url "&key=" google-api-key))]
      (if (or error (not= status 200))
        (throw (ex-info (format "Google API call (%s) failed" (str api-url "&key=REDACTED")) {:status status :body body} error))
        (let [api-response (ch/parse-string body u/clojurise-json-key)
              items        (:items api-response)
              next-page    (:next-page-token api-response)]
          (if (not (s/blank? next-page))
            (recur (format (str api-host endpoint-youtube-search-page-n) channel-id next-page)
                   (inc page)
                   (into result items))
            (into result items)))))))

(defn exit
  [msg code]
  (binding [*out* *err*]
    (println msg)
    (flush))
  (System/exit code))

(defn words
  [titles]
  (s/split (u/replace-all (s/join " 🔚 " titles)
                          [["&amp;"             "&"]
                           ["&quot;"            "\""]
                           ["&#39;"             "'"]
                           ["’"                 "'"]
                           [#"[“”]"             "\""]
                           [#"([!?:;,\"…\*])"   " $1 "]
                           [#"((?<!\s))(\.+)\s" "$1 $2 "]
                          ])
           #"\s+"))

(defn gen-chain
  ([google-api-key youtube-channel-id] (gen-chain (all-videos-for-channel google-api-key youtube-channel-id)))
  ([videos]
   (let [titles (map #(:title (:snippet %)) videos)
         words  (words titles)]
     (mc/collate words 1))))    ;1 = extra deranged IST mode, 2 = relatively sane IST mode

(defn -main
  [& args]
  (try
    (if (not= 1 (count args))
      (exit "Please provide a Google API key on the command line." -1))

    (let [google-api-key (first args)
          chain          (gen-chain google-api-key ist-youtube-channel-id)]
      (with-open [w (io/writer (io/file "resources/ist-markov-chain.edn"))]
        (pp/pprint chain w)))
    (catch Exception e
      (exit e -1))))
