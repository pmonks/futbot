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

(ns futbot.source.pro
  (:require [java-time :as tm]
            [remus     :as rss]))

(def pro-blog-feed-url "http://proreferees.com/feed/atom/")   ; I prefer atom...

(defn posts
  "Returns a sequence of maps representing all of PRO's posts, optionally for the given categories and/or published since the given date, or nil if there aren't any."
  [& {:keys [categories since]}]
  (when-let [blog-entries (seq (:entries (:feed (rss/parse-url pro-blog-feed-url))))]
    (let [blog-entries (if categories
                         (filter #(some categories (map :name (:categories %))) blog-entries)
                         blog-entries)
          blog-entries (if since
                         (filter #(tm/after? (tm/instant (:published-date %)) (tm/instant since)) blog-entries)
                         blog-entries)]
      (seq blog-entries))))

(def categories-of-interest #{"PRO Insight"
                              "The Definitive Angle"
                              "VAR a Fondo"})

(defn posts-of-interest-since
  "Returns a sequence of maps representing all of PRO's posts, optionally for the given categories and/or published since the given date, or nil if there aren't any."
  [since]
  (posts :categories categories-of-interest :since since))
