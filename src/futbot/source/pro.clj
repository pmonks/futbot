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

(def pro-blog-rss-feed-url "http://proreferees.com/feed/atom/")   ; I prefer atom...
(def pro-insight-category  "PRO Insight")

(defn insights
  "Returns a sequence of maps representing all of the PRO Insights posted to PRO's atom feed, optionally since the given date, or nil if there aren't any."
  ([] (insights nil))
  ([since]
    (seq
      (when-let [all-pro-insights (filter #(some #{pro-insight-category} (map :name (:categories %)))
                                          (:entries (:feed (rss/parse-url pro-blog-rss-feed-url))))]
        (if since
          (filter #(tm/after? (tm/instant (:published-date %)) (tm/instant since)) all-pro-insights)
          all-pro-insights)))))
