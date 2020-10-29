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

(ns futbot.source.cnra
  (:require [clojure.string :as s]
            [java-time      :as tm]
            [remus          :as rss]
            [futbot.util    :as u]))

(def cnra-rss-feed-url "http://www.cnra.net/feed/atom/")   ; I prefer atom...
(def quiz-post-title   "Monthly Video Quizzes")

(defn para-to-quiz
  [^org.jsoup.nodes.Element p]
  (let [text               (u/replace-all (.text p)
                                         [[#"[–‑‒–—]" "-"]])  ; Normalise Unicode "dash" characters
       [month-year topic] (s/split text #" - ")   ; NOTE: ASSUMES QUIZ LINK PARAS ARE CONSISTENT - NEED TO CONFIRM ON NOV 15 WHEN THEY PUBLISH A SECOND ONE!
       date               (tm/local-date "dd MMMM yyyy" (str "15 " month-year))
       link               (.attr (.selectFirst p "a[href*=forms.gle]") "href")]      ; Note: assumes the quiz is always the first link to a Google form
    (when link
      {
        :date  date
        :topic (s/lower-case topic)
        :link  link
      })))

(defn html-to-quizzes
  [html]
  (let [quiz-paras (drop-last (drop 2 (.select (org.jsoup.Jsoup/parse html) "p")))]   ; NOTE: ASSUMES PAGE STRUCTURE STAYS CONSISTENT - NEED TO CONFIRM ON NOV 15 WHEN THEY PUBLISH A SECOND ONE!
    (keep identity (map para-to-quiz quiz-paras))))

(defn quizzes
  "Returns a sequence of maps representing all of the video quizzes posted by CNRA, optionally since the given date, or nil if there aren't any."
  ([] (quizzes nil))
  ([since]
    (seq
      (when-let [all-quizzes (html-to-quizzes (:value (first (:contents (first (filter #(= (:title %) quiz-post-title)
                                                                                       (:entries (:feed (rss/parse-url cnra-rss-feed-url)))))))))]
        (if since
          (filter #(tm/after? (:date %) since) all-quizzes)
          all-quizzes)))))
