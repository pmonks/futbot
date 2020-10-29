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

(ns futbot.source.dutch-referee-blog
  (:require [clojure.string :as s]
            [java-time      :as tm]
            [remus          :as rss]))

(def dutch-referee-blog-rss-feed-url "http://www.dutchreferee.com/feed/atom")   ; I prefer atom...
(def quiz-title-substring            "Laws of the Game Quiz")

(defn quizzes
  "Returns a sequence of maps representing all of the quizzes in the Dutch Referee Blog's RSS feed, optionally since the given date, or nil if there aren't any."
  ([] (quizzes nil))
  ([since]
    (seq
      (when-let [all-quizzes (filter #(s/index-of (:title %) quiz-title-substring)
                                     (:entries (:feed (rss/parse-url dutch-referee-blog-rss-feed-url))))]
        (if since
          (filter #(tm/after? (tm/instant (:published-date %)) (tm/instant since)) all-quizzes)
          all-quizzes)))))
