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

(ns futbot.pdf
  (:require [clojure.string        :as s]
            [clojure.java.io       :as io]
            [clojure.tools.logging :as log]
            [java-time             :as tm]
            [clj-pdf.core          :as pdf]
            [futbot.flags          :as fl]))

(defn generate-daily-schedule-pdf-data-structure
  [day matches]
  [{:title         (str "Soccer Matches - " (tm/format "yyyy-MM-dd" day))
    :creator       "futbot"
    :author        "futbot"
    :size          "a4"
    :right-margin  25
    :bottom-margin 25
    :left-margin   25
    :top-margin    25
    :font          {:family   :helvetica
                    :encoding :unicode
                    :size     10}}

    [:heading (str (count matches) " matches scheduled for " (tm/format "EEEE LLLL d, yyyy" day) ":")]

    ; Match table
    (loop [last-utc-date     nil
           current-match     (first matches)
           remaining-matches (rest matches)
           result            (atom [:table {:cell-border    true
                                            :no-split-rows? true
                                            :leading        10
                                            :header         [{:background-color [200 200 200]} "Competition" "Home Team" "Away Team"]}])]
      (if current-match
        (do
          ; Control-break row when we see a new utc-date
          (if (not= last-utc-date (:utc-date current-match))
            (let [utc-date-txt    (tm/format "h:mm a" (tm/zoned-date-time (:utc-date current-match)))
                  utc-date-txt-qs (java.net.URLEncoder/encode ^String utc-date-txt "UTF-8")]
              (swap! result conj [[:cell {:colspan 3 :style :bold :background-color [230 230 230]}
                                  "Scheduled start "
                                  [:anchor {:style {:styles [:bold :underline] :color [0 123 255]}
                                            :target (str "https://www.thetimezoneconverter.com/?t=" utc-date-txt-qs "&tz=UTC")}
                                           (str utc-date-txt " UTC")]
                                  ":"]])))

          ; Regular data row
          (let [country   (s/trim (get-in current-match [:competition :area :code]))
                flag-file (fl/image-file country)]
            (swap! result conj [[:cell [:chunk (if flag-file [:image {:scale 2} (.toURL flag-file)])]
                                       (str " " (get-in current-match [:competition :name] "Unknown"))]
                                (get-in current-match [:home-team :name]   "Unknown")
                                (get-in current-match [:away-team :name]   "Unknown")]))

          (recur (:utc-date current-match)
                 (first remaining-matches)
                 (rest  remaining-matches)
                 result))
        @result))

    [:paragraph [:anchor {:style {:styles [:bold :underline] :color [0 123 255]}
                          :target "https://livesoccertv.com/"}
                         "Find out how to watch these matches."]]])

(defn generate-daily-schedule
  [day matches]
  (log/debug (str "Daily schedule PDF generation started for " (tm/format "yyyy-MM-dd" day) ": " (count matches) " matches."))
  (if (pos? (count matches))
    (let [pdf-data         (generate-daily-schedule-pdf-data-structure day matches)
          temp-pdf-file    (doto (java.io.File/createTempFile "futbot-tmp-" ".pdf") (.deleteOnExit))]
      (with-open [temp-pdf-file-os (io/output-stream temp-pdf-file)]
        (pdf/pdf pdf-data
                 temp-pdf-file-os))
      temp-pdf-file)))
