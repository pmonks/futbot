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
  (:require [clojure.string     :as s]
            [java-time          :as tm]
            [org.httpkit.client :as http]
            [futbot.util        :as u]))

(def cnra-quiz-page-url "http://www.cnra.net/monthly-video-quizzes/")

(defn- retrieve-and-parse-quiz-page
  "Retrieves the CNRA quiz page and returns a JSoup-parsed represention. Throws an ex-info on failure."
  []
  (let [{:keys [status headers body error]} @(http/get cnra-quiz-page-url)]
    (if error
      (throw (ex-info (str "Error while retrieving CNRA quiz page " cnra-quiz-page-url) {:status status :headers headers :body body} error))
      (org.jsoup.Jsoup/parse body))))

(defn- para-to-quiz
  [^org.jsoup.nodes.Element p]
  (let [text               (u/replace-all (.text p)
                                         [[#"[–‑‒–—]" "-"]])  ; Normalise Unicode "dash" characters
       [month-year topic] (s/split text #" - ")
       date               (tm/local-date "dd MMMM yyyy" (str "15 " month-year))
       link               (.attr (.selectFirst p "a[href*=forms.gle]") "href")]      ; Note: assumes the quiz is always the first link to a Google form
    (when link
      {
        :date      date
        :quiz-date month-year
        :topic     (s/lower-case topic)
        :link      link
      })))

(defn- html-to-quizzes
  [^org.jsoup.nodes.Document page]
  (let [quiz-para  (.nextElementSibling ^org.jsoup.nodes.Element (first (.select page "hr[class=wp-block-separator]")))  ; Find the (single) paragraph containing the quizes
        quiz-paras (.select (org.jsoup.Jsoup/parse (s/replace (str quiz-para) "<br>" "</p><p>")) "p")]                   ; Convert it into one paragraph per quiz
    (keep identity (map para-to-quiz quiz-paras))))

(defn quizzes
  "Returns a sequence of maps representing all of the video quizzes posted by CNRA, optionally since the given date, or nil if there aren't any."
  ([] (quizzes nil))
  ([since]
    (seq
      (when-let [all-quizzes (html-to-quizzes (retrieve-and-parse-quiz-page))]
        (if since
          (filter #(tm/after? (:date %) since) all-quizzes)
          all-quizzes)))))
