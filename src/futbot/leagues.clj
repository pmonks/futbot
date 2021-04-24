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

(ns futbot.leagues
  (:require [clojure.string :as s]))

(def ^:private league-logo-urls {
  ; Domestic Leagues
  "premier league"        "https://upload.wikimedia.org/wikipedia/en/thumb/f/f2/Premier_League_Logo.svg/320px-Premier_League_Logo.svg.png"
  "championship"          "https://upload.wikimedia.org/wikipedia/en/3/37/EFL_Championship.png"
  "fa cup"                "https://upload.wikimedia.org/wikipedia/en/5/55/FA_Cup_2020.png"
  "bundesliga"            "https://upload.wikimedia.org/wikipedia/en/thumb/d/df/Bundesliga_logo_%282017%29.svg/480px-Bundesliga_logo_%282017%29.svg.png"
  "primera division"      "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6e/LaLiga_Santander_%282%29.svg/561px-LaLiga_Santander_%282%29.svg.png"
  "serie a"               "https://upload.wikimedia.org/wikipedia/en/thumb/e/e1/Serie_A_logo_%282019%29.svg/272px-Serie_A_logo_%282019%29.svg.png" ; Italy
  "ligue 1"               "https://upload.wikimedia.org/wikipedia/en/thumb/b/ba/Ligue_1_Uber_Eats.svg/327px-Ligue_1_Uber_Eats.svg.png"
  "eredivisie"            "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0f/Eredivisie_nieuw_logo_2017-.svg/640px-Eredivisie_nieuw_logo_2017-.svg.png"
  "primeira liga"         "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0e/Liga_NOS_logo.png/358px-Liga_NOS_logo.png"
  "série a"               "https://upload.wikimedia.org/wikipedia/en/4/42/Campeonato_Brasileiro_S%C3%A9rie_A_logo.png" ; Brazil
  "mls"                   "https://upload.wikimedia.org/wikipedia/commons/thumb/7/76/MLS_crest_logo_RGB_gradient.svg/454px-MLS_crest_logo_RGB_gradient.svg.png"
  "a league"              "https://upload.wikimedia.org/wikipedia/en/thumb/4/42/A-League_logo.svg/408px-A-League_logo.svg.png"

  ; International leagues
  "fifa world cup"        "https://upload.wikimedia.org/wikipedia/en/thumb/e/e3/2022_FIFA_World_Cup.svg/402px-2022_FIFA_World_Cup.svg.png"
  "uefa champions league" "https://upload.wikimedia.org/wikipedia/en/thumb/b/bf/UEFA_Champions_League_logo_2.svg/497px-UEFA_Champions_League_logo_2.svg.png"
;  "european championship" ""  ; Next one is in 2024
  "uefa europa league"    "https://upload.wikimedia.org/wikipedia/en/thumb/0/03/Europa_League.svg/334px-Europa_League.svg.png"
})

(defn league-to-logo-url
  "Returns the URL of the given league's logo (or nil if one isn't available)."
  [league-name]
  (get league-logo-urls (s/lower-case (s/trim league-name))))
