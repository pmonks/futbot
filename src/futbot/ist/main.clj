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
            [clojure.edn        :as edn]
            [clojure.pprint     :as pp]
            [clojure.stacktrace :as st]
            [org.httpkit.client :as http]
            [cheshire.core      :as ch]
            [java-time          :as tm]
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
    (print "Reading page" page)
    (let [{:keys [status body error]} @(http/get (str api-url "&key=" google-api-key))]
      (if (or error (not= status 200))
        (throw (ex-info (format "Google API call (%s) failed" (str api-url "&key=REDACTED")) {:status status :body body} error))
        (let [api-response (ch/parse-string body u/clojurise-json-key)
              items        (:items api-response)
              next-page    (:next-page-token api-response)]
          (println "," (count items) "items found")
          (if (and (= (count items) 50)
                   (not (s/blank? next-page)))
            (recur (format (str api-host endpoint-youtube-search-page-n) channel-id next-page)
                   (inc page)
                   (into result items))
            (into result items)))))))

(defn tokenize
  "Convert the given video titles into tokens (~= words) suitable for collation into a Markov chain."
  [titles]
  (s/split (u/replace-all (s/join " 🔚 " titles)
                          [["&amp;"             "&"]
                           ["&quot;"            "\""]
                           ["&#39;"             "'"]
                           ["’"                 "'"]
                           [#"[“”]"             "\""]
                           ["…"                 "..."]
                           [#"([!?:;,\"\*])"    " $1 "]
                           [#"\s&(\S)"          " & $1"]
                           [#"(\S)&\s"          "$1 & "]
                           [#"(\D)(\.+)\s"      "$1 $2 "]
                           [#"\s-(\S)"          " - $1"]
                          ])
           #"\s+"))

(defn gen-chain
  "Generate a Markov chain for the given video titles."
  [titles]
  (mc/collate (tokenize titles) 1))    ;1 = extra deranged IST mode, 2 = relatively sane IST mode

(defn load-ist-titles
  "Loads titles from Youtube, and caches them to disk, reusing that cache if it already exists.  We do this because Google's API call quotas are draconian."
  [google-api-key youtube-channel-id]
  (let [titles-filename (str "titles-" (tm/local-date) ".edn")
        titles-file     (io/file titles-filename)]
    (if (.exists titles-file)
      (do
        (println titles-filename "found, loading it...")
        (edn/read-string (slurp titles-file)))
      (do
        (println titles-filename "not found, reading titles from YouTube...")
        (let [titles (map #(:title (:snippet %)) (all-videos-for-channel google-api-key youtube-channel-id))]
          (println "Caching" (count titles) "titles to" titles-filename)
          (with-open [w (io/writer titles-file)]
            (pp/pprint titles w))
          titles)))))

(defn load-bonus-titles
  []
  (if-let [bonus-file (io/resource "bonus-titles.edn")]
    (do
      (println "bonus-titles.edn found, reading titles...")
      (edn/read-string (slurp bonus-file)))))

(defn -main
  [& args]
  (try
    (if (not= 1 (count args))
      (u/exit -1 "Please provide a Google API key on the command line."))

    (let [google-api-key (first args)
          ist-titles     (load-ist-titles google-api-key ist-youtube-channel-id)
          bonus-titles   (load-bonus-titles)
          _              (println (count ist-titles) "IST titles, and" (count bonus-titles) "bonus titles loaded")
          chain          (gen-chain (into ist-titles bonus-titles))
          chain-filename "resources/ist-markov-chain.edn"]
      (println "Writing Markov chain to" (str chain-filename "..."))
      (with-open [w (io/writer (io/file chain-filename))]
        (pp/pprint chain w)))

    (println "Done.")
    (catch Exception e
      (u/exit -1 (with-out-str (st/print-stack-trace e))))
    (finally
      (u/exit))))
