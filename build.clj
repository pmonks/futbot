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

(ns build
  "Build script for futbot.

For more information, run:

clojure -A:deps -T:build help/doc"
  (:require [org.corfield.build :as bb]
            [pbr.tasks          :as pbr]))

(def lib       'com.github.pmonks/fubot)
(def version   (format "1.0.%s" (.format (java.text.SimpleDateFormat. "yyyyMMdd") (java.util.Date.))))
(def uber-file "./target/futbot-standalone.jar")
(def main      'futbot.main)

; Utility fns
(defn set-opts
  [opts]
  (assoc opts
         :lib              lib
         :version          version
         :uber-file        uber-file
         :main             main
         :deploy-info-file "./resources/deploy-info.edn"
         :write-pom        true
         :pom              {:description      "A Discord bot that delivers football (soccer) information to Discord."
                            :url              "https://github.com/pmonks/futbot"
                            :licenses         [:license   {:name "Apache License 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}]
                            :developers       [:developer {:id "pmonks" :name "Peter Monks" :email "pmonks+futbot@gmail.com"}]
                            :scm              {:url "https://github.com/pmonks/futbot" :connection "scm:git:git://github.com/pmonks/futbot.git" :developer-connection "scm:git:ssh://git@github.com/pmonks/futbot.git"}
                            :issue-management {:system "github" :url "https://github.com/pmonks/futbot/issues"}}))

; Development-time tasks
(defn clean
  "Clean up the project."
  [opts]
  (bb/clean (set-opts opts)))

(defn uber
  "Create an uber jar."
  [opts]
  (-> opts
    (set-opts)
    (pbr/pom)
    (bb/uber)))

(defn check
  "Check the code by compiling it."
  [opts]
  (bb/run-task (set-opts opts) [:check]))

(defn outdated
  "Check for outdated dependencies."
  [opts]
  (bb/run-task (set-opts opts) [:outdated]))

(defn licenses
  "Display all dependencies' licenses."
  [opts]
  (pbr/licenses opts))

(defn kondo
  "Run the clj-kondo linter."
  [opts]
  (bb/run-task (set-opts opts) [:kondo]))

(defn eastwood
  "Run the eastwood linter."
  [opts]
  (bb/run-task (set-opts opts) [:eastwood]))

(defn lint
  "Run all linters."
  [opts]
  (-> opts
    (kondo)
    (eastwood)))

(defn ci
  "Run the CI pipeline."
  [opts]
  (-> opts
    (outdated)
    (check)
    (lint)))

(defn check-release
  "Check that a release can be done from the current directory."
  [opts]
  (-> opts
      (set-opts)
      (ci)
      (pbr/check-release)))

(defn release
  "Release a new version of the bot."
  [opts]
  (-> opts
      (set-opts)
      (pbr/release)))
