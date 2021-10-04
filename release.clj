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

(ns release
  "Release script for futbot.

For more information, run:

clojure -A:deps -T:release help/doc"
  (:require [clojure.string  :as s]
            [clojure.java.io :as io]
            [clojure.pprint  :as pp]
            [build           :refer [version ci exec]]))

(defn- ensure-command
  "Ensures that the given command is available."
  [command]
  (exec ["command" "-v" command] {:out :capture :err :capture}))

(defn check
  "Check that a release can be done from the current directory."
  [opts]
  (ensure-command "git")
  (ensure-command "hub")
  (ensure-command "xmlstarlet")

  (let [current-branch (s/trim (:out (exec "git branch --show-current" {:out :capture :err :capture})))]
    (when-not (= "dev" current-branch)
      (throw (ex-info (str "Must be on branch 'dev' to prepare a release, but current branch is '" current-branch "'.") {}))))

  (let [git-status (exec "git status --short" {:out :capture :err :capture})]
    (when (or (not (s/blank? (:out git-status)))
              (not (s/blank? (:err git-status))))
      (throw (ex-info (str "Working directory is not clean:\n" (:out git-status) "Please commit, revert, or stash these changes before preparing a release.") git-status))))

  (exec "git fetch origin main:main")
  (exec "git merge main")
  (exec "git pull")

  (ci opts))


(defn release
  "Release a new version of the bot."
  [opts]
  (println (str "▶️ Releasing futbot v" version "..."))

  (println "ℹ️ Checking that a release can be made...")
  (check opts)

  (println "⏸ All checks ok, press any key to continue, or Ctrl+C to quit...")
  (flush)
  (read-line)

  (println "ℹ️ Updating version in pom.xml...")
  (exec ["xmlstarlet" "ed" "--inplace" "-N" "pom='http://maven.apache.org/POM/4.0.0'" "-u" "'/pom:project/pom:version'" "-v" version "pom.xml"])
  (exec ["git" "commit" "-m" (str ":gem: Release v" version) "pom.xml"])

  (println "ℹ️ Tagging release...")
  (exec ["git" "tag" "-f" "-a" (str "v" version) "-m" (str "Release v" version)])

  (println "ℹ️ Updating deploy info...")
  (with-open [w (io/writer (io/file "resources/deploy-info.edn"))]
    (pp/pprint {
                 :hash (:out (exec (str "git show-ref -s --tags v" version) {:out :capture}))
                 :tag  (str "v" version)
                 :date (str "#inst " (java.time.Instant/now))
               }
               w))
  (exec ["git" "commit" "-m" (str ":gem: Release v" version) "resources/deploy-info.edn"])

  (println "ℹ️ Pushing changes...")
  (exec "git push")
  (exec "git push origin -f --tags")

  (println "ℹ️ Creating pull request...")
  (exec ["hub" "pull-request" "--browse" "-f"
         "-m" (str "Release v" version)
         "-m" (str "futbot release v" version ". See commit log for details of what's included in this release.")
         "-h" "dev" "-b" "main"])

  (println "ℹ️ After the PR has been merged, it is highly recommended to:\n"
           "  1. git fetch origin main:main\n"
           "  2. git merge main\n"
           "  3. git pull\n"
           "  4. git push")

  (println "⏹ Done."))

