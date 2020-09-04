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

(ns futbot.daily-schedule.main
  (:require [clojure.stacktrace   :as st]
            [java-time            :as tm]
            [futbot.football-data :as fd]
            [futbot.pdf           :as pdf]
            [futbot.util          :as u]))

(defn -main
  [& args]
  (try
    (if (not= 1 (count args))
      (u/exit -1 "Please provide a football-data.org API token on the command line."))

    (let [football-data-api-token (first args)
          day                     (tm/with-clock (tm/system-clock "UTC") (tm/zoned-date-time))
          matches                 (fd/matches-on-day football-data-api-token day)
          pdf-filename            (str "daily-schedule-" (tm/format "yyyy-MM-dd" day) ".pdf")]
      (if (seq matches)
        (do
          (println "Writing daily schedule to" (str pdf-filename "..."))
          (pdf/generate-daily-schedule (partial fd/match football-data-api-token) day matches pdf-filename))
        (println "No matches found for today, skipping generation of daily schedule.")))

    (println "Done.")
    (catch Exception e
      (u/exit -1 (with-out-str (st/print-stack-trace e))))
    (finally
      (u/exit))))
