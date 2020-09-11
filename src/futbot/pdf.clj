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

(defn zero-if-nil
  [x]
  (if (nil? x)
    0
    x))

(defn generate-daily-schedule-pdf-data-structure
  [match-details-fn day matches]
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
                                            :header         [{:background-color [200 200 200]} "Competition" "Home" "Away" "Results this Season\n(HW / AW / D / G)"]}])]
      (if current-match
        (do
          ; Control-break row when we see a new utc-date
          (when (not= last-utc-date (:utc-date current-match))
            (let [utc-date-txt    (tm/format "h:mm a" (tm/zoned-date-time (:utc-date current-match)))
                  utc-date-txt-qs (java.net.URLEncoder/encode ^String utc-date-txt "UTF-8")]
              (swap! result conj [[:cell {:colspan 4 :style :bold :background-color [230 230 230]}
                                  "Scheduled start "
                                  [:anchor {:style {:styles [:bold :underline] :color [0 123 255]}
                                            :target (str "https://www.thetimezoneconverter.com/?t=" utc-date-txt-qs "&tz=UTC")}
                                           (str utc-date-txt " UTC")]
                                  ":"]])))

          ; Regular data row
          (let [country   (s/trim (get-in current-match [:competition :area :code]))
                flag-url  (fl/image-url country)
                head2head (:head2head (match-details-fn (:id current-match)))]
            (swap! result conj [[:cell [:chunk (when flag-url [:image {:scale 2} flag-url])]
                                       (str " " (get-in current-match [:competition :name] "Unknown"))]
                                (get-in current-match [:home-team :name] "Unknown")
                                (get-in current-match [:away-team :name] "Unknown")
                                (str (zero-if-nil (get-in head2head [:home-team :wins])) " / "
                                     (zero-if-nil (get-in head2head [:away-team :wins])) " / "
                                     (zero-if-nil (get-in head2head [:home-team :draws])) " / "
                                     (zero-if-nil (:total-goals head2head)))]))

          (recur (:utc-date current-match)
                 (first remaining-matches)
                 (rest  remaining-matches)
                 result))
        @result))

    [:paragraph [:anchor {:style {:styles [:underline] :color [0 123 255]}
                          :target "https://livesoccertv.com/"}
                         "Find out how to watch these matches."]]])

(defn generate-daily-schedule
  "Generates the daily schedule PDF for the given day and matches, returning the generated file (as a java.io.File)."
  ([match-details-fn day matches] (generate-daily-schedule match-details-fn day matches (doto (java.io.File/createTempFile "futbot-tmp-" ".pdf") (.deleteOnExit))))
  ([match-details-fn day matches pdf-file]
   (log/debug (str "Daily schedule PDF generation started for " (tm/format "yyyy-MM-dd" day) ": " (count matches) " matches."))
   (when (pos? (count matches))
     (let [pdf-data  (generate-daily-schedule-pdf-data-structure match-details-fn day matches)]
       (with-open [pdf-file-os (io/output-stream pdf-file)]
         (pdf/pdf pdf-data
                  pdf-file-os))
       (io/file pdf-file)))))
