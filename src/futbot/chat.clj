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

(ns futbot.chat
  (:require [clojure.string        :as s]
            [clojure.java.io       :as io]
            [clojure.tools.logging :as log]
            [java-time             :as tm]
            [futbot.football-data  :as fd]
            [discljord.connections :as dc]
            [discljord.messaging   :as dm]
            [discljord.events      :as de]))

; Responsive fns
(defmulti handle-discord-event
  "Discord event handler"
  (fn [event-type event-data]
    event-type))

; Default Discord event handler (noop)
(defmethod handle-discord-event :default
  [event-type event-data])

; Note: no responsive fns have been implemented yet