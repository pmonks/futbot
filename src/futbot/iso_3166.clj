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

(ns futbot.iso-3166
  (:require [clojure.string :as s]
            [clojure.set    :as set]))

(def ^:private name-to-alpha-3-map {
  "aruba"                                                    "ABW"
  "afghanistan"                                              "AFG"
  "angola"                                                   "AGO"
  "anguilla"                                                 "AIA"
  "åland islands"                                            "ALA"
  "aland islands"                                            "ALA"
  "albania"                                                  "ALB"
  "andorra"                                                  "AND"
  "united arab emirates"                                     "ARE"
  "the united arab emirates"                                 "ARE"
  "argentina"                                                "ARG"
  "armenia"                                                  "ARM"
  "american samoa"                                           "ASM"
  "antarctica"                                               "ATA"
  "french southern territories"                              "ATF"
  "the french southern territories"                          "ATF"
  "antigua and barbuda"                                      "ATG"
  "australia"                                                "AUS"
  "austria"                                                  "AUT"
  "azerbaijan"                                               "AZE"
  "burundi"                                                  "BDI"
  "belgium"                                                  "BEL"
  "benin"                                                    "BEN"
  "bonaire, sint eustatius and saba"                         "BES"
  "burkina faso"                                             "BFA"
  "bangladesh"                                               "BGD"
  "bulgaria"                                                 "BGR"
  "bahrain"                                                  "BHR"
  "bahamas"                                                  "BHS"
  "the bahamas"                                              "BHS"
  "bosnia and herzegovina"                                   "BIH"
  "saint barthélemy"                                         "BLM"
  "saint barthelemy"                                         "BLM"
  "belarus"                                                  "BLR"
  "belize"                                                   "BLZ"
  "bermuda"                                                  "BMU"
  "bolivia"                                                  "BOL"
  "plurinational state of bolivia"                           "BOL"
  "brazil"                                                   "BRA"
  "barbados"                                                 "BRB"
  "brunei darussalam"                                        "BRN"
  "bhutan"                                                   "BTN"
  "bouvet island"                                            "BVT"
  "botswana"                                                 "BWA"
  "central african republic"                                 "CAF"
  "the central african republic"                             "CAF"
  "canada"                                                   "CAN"
  "cocos (keeling) islands"                                  "CCK"
  "the cocos (keeling) islands"                              "CCK"
  "switzerland"                                              "CHE"
  "chile"                                                    "CHL"
  "china"                                                    "CHN"
  "côte d'ivoire"                                            "CIV"
  "cote d'ivoire"                                            "CIV"
  "ivory coast"                                              "CIV"
  "cameroon"                                                 "CMR"
  "democratic republic of the congo"                         "COD"
  "the democratic republic of the congo"                     "COD"
  "congo"                                                    "COG"
  "the congo"                                                "COG"
  "cook islands"                                             "COK"
  "the cook islands"                                         "COK"
  "colombia"                                                 "COL"
  "comoros"                                                  "COM"
  "the comoros"                                              "COM"
  "cabo verde"                                               "CPV"
  "costa rica"                                               "CRI"
  "cuba"                                                     "CUB"
  "curaçao"                                                  "CUW"
  "curacao"                                                  "CUW"
  "christmas island"                                         "CXR"
  "cayman islands"                                           "CYM"
  "the cayman islands"                                       "CYM"
  "cyprus"                                                   "CYP"
  "czechia"                                                  "CZE"
  "germany"                                                  "DEU"
  "djibouti"                                                 "DJI"
  "dominica"                                                 "DMA"
  "denmark"                                                  "DNK"
  "dominican republic"                                       "DOM"
  "the dominican republic"                                   "DOM"
  "algeria"                                                  "DZA"
  "ecuador"                                                  "ECU"
  "egypt"                                                    "EGY"
  "eritrea"                                                  "ERI"
  "western sahara"                                           "ESH"
  "spain"                                                    "ESP"
  "estonia"                                                  "EST"
  "ethiopia"                                                 "ETH"
  "finland"                                                  "FIN"
  "fiji"                                                     "FJI"
  "falkland islands"                                         "FLK"
  "the falkland islands"                                     "FLK"
  "malvinas"                                                 "FLK"
  "france"                                                   "FRA"
  "faroe islands"                                            "FRO"
  "the faroe islands"                                        "FRO"
  "micronesia"                                               "FSM"
  "federated states of micronesia"                           "FSM"
  "the federated states of micronesia"                       "FSM"
  "gabon"                                                    "GAB"
  "uk"                                                       "GBR"
  "u.k."                                                     "GBR"
  "united kingdom"                                           "GBR"
  "great britain"                                            "GBR"
  "united kingdom of great britain and northern ireland"     "GBR"
  "the united kingdom of great britain and northern ireland" "GBR"
  "georgia"                                                  "GEO"
  "guernsey"                                                 "GGY"
  "ghana"                                                    "GHA"
  "gibraltar"                                                "GIB"
  "guinea"                                                   "GIN"
  "guadeloupe"                                               "GLP"
  "gambia"                                                   "GMB"
  "the gambia"                                               "GMB"
  "guinea-bissau"                                            "GNB"
  "guinea bissau"                                            "GNB"
  "equatorial guinea"                                        "GNQ"
  "greece"                                                   "GRC"
  "grenada"                                                  "GRD"
  "greenland"                                                "GRL"
  "guatemala"                                                "GTM"
  "french guiana"                                            "GUF"
  "guam"                                                     "GUM"
  "guyana"                                                   "GUY"
  "hong kong"                                                "HKG"
  "heard island and mcdonald islands"                        "HMD"
  "honduras"                                                 "HND"
  "croatia"                                                  "HRV"
  "haiti"                                                    "HTI"
  "hungary"                                                  "HUN"
  "indonesia"                                                "IDN"
  "isle of man"                                              "IMN"
  "india"                                                    "IND"
  "british indian ocean territory"                           "IOT"
  "the british indian ocean territory"                       "IOT"
  "ireland"                                                  "IRL"
  "iran"                                                     "IRN"
  "islamic republic of iran"                                 "IRN"
  "the islamic republic of iran"                             "IRN"
  "iraq"                                                     "IRQ"
  "iceland"                                                  "ISL"
  "israel"                                                   "ISR"
  "italy"                                                    "ITA"
  "jamaica"                                                  "JAM"
  "jersey"                                                   "JEY"
  "jordan"                                                   "JOR"
  "japan"                                                    "JPN"
  "kazakhstan"                                               "KAZ"
  "kenya"                                                    "KEN"
  "kyrgyzstan"                                               "KGZ"
  "cambodia"                                                 "KHM"
  "kiribati"                                                 "KIR"
  "saint kitts and nevis"                                    "KNA"
  "korea"                                                    "KOR"
  "south korea"                                              "KOR"
  "republic of korea"                                        "KOR"
  "the republic of korea"                                    "KOR"
  "kuwait"                                                   "KWT"
  "lao"                                                      "LAO"
  "laos"                                                     "LAO"
  "lao peoples democratic republic"                          "LAO"
  "lao people's democratic republic"                         "LAO"
  "the lao peoples democratic republic"                      "LAO"
  "the lao people's democratic republic"                     "LAO"
  "lebanon"                                                  "LBN"
  "liberia"                                                  "LBR"
  "libya"                                                    "LBY"
  "saint lucia"                                              "LCA"
  "liechtenstein"                                            "LIE"
  "sri lanka"                                                "LKA"
  "lesotho"                                                  "LSO"
  "lithuania"                                                "LTU"
  "luxembourg"                                               "LUX"
  "latvia"                                                   "LVA"
  "macao"                                                    "MAC"
  "saint martin"                                             "MAF"
  "morocco"                                                  "MAR"
  "monaco"                                                   "MCO"
  "moldova"                                                  "MDA"
  "republic of moldova"                                      "MDA"
  "the republic of moldova"                                  "MDA"
  "madagascar"                                               "MDG"
  "maldives"                                                 "MDV"
  "mexico"                                                   "MEX"
  "marshall islands"                                         "MHL"
  "the marshall islands"                                     "MHL"
  "macedonia"                                                "MKD"
  "north macedonia"                                          "MKD"
  "republic of north macedonia"                              "MKD"
  "the republic of north macedonia"                          "MKD"
  "mali"                                                     "MLI"
  "malta"                                                    "MLT"
  "myanmar"                                                  "MMR"
  "montenegro"                                               "MNE"
  "mongolia"                                                 "MNG"
  "northern mariana islands"                                 "MNP"
  "the northern mariana islands"                             "MNP"
  "mozambique"                                               "MOZ"
  "mauritania"                                               "MRT"
  "montserrat"                                               "MSR"
  "martinique"                                               "MTQ"
  "mauritius"                                                "MUS"
  "malawi"                                                   "MWI"
  "malaysia"                                                 "MYS"
  "mayotte"                                                  "MYT"
  "namibia"                                                  "NAM"
  "new caledonia"                                            "NCL"
  "niger"                                                    "NER"
  "the niger"                                                "NER"
  "norfolk island"                                           "NFK"
  "nigeria"                                                  "NGA"
  "nicaragua"                                                "NIC"
  "niue"                                                     "NIU"
  "netherlands"                                              "NLD"
  "the netherlands"                                          "NLD"
  "norway"                                                   "NOR"
  "nepal"                                                    "NPL"
  "nauru"                                                    "NRU"
  "new zealand"                                              "NZL"
  "oman"                                                     "OMN"
  "pakistan"                                                 "PAK"
  "panama"                                                   "PAN"
  "pitcairn"                                                 "PCN"
  "peru"                                                     "PER"
  "philippines"                                              "PHL"
  "the philippines"                                          "PHL"
  "palau"                                                    "PLW"
  "papua new guinea"                                         "PNG"
  "poland"                                                   "POL"
  "puerto rico"                                              "PRI"
  "north korea"                                              "PRK"
  "democratic people's republic of korea"                    "PRK"
  "the democratic people's republic of korea"                "PRK"
  "portugal"                                                 "PRT"
  "paraguay"                                                 "PRY"
  "palestine"                                                "PSE"
  "state of palestine"                                       "PSE"
  "the state of palestine"                                   "PSE"
  "french polynesia"                                         "PYF"
  "qatar"                                                    "QAT"
  "réunion"                                                  "REU"
  "reunion"                                                  "REU"
  "romania"                                                  "ROU"
  "russia"                                                   "RUS"
  "russian federation"                                       "RUS"
  "the russian federation"                                   "RUS"
  "rwanda"                                                   "RWA"
  "saudi arabia"                                             "SAU"
  "kingdom of saudi arabia"                                  "SAU"
  "the kingdom of saudi arabia"                              "SAU"
  "sudan"                                                    "SDN"
  "the sudan"                                                "SDN"
  "senegal"                                                  "SEN"
  "singapore"                                                "SGP"
  "south georgia and the south sandwich islands"             "SGS"
  "saint helena, ascension and tristan da cunha"             "SHN"
  "svalbard and jan mayen"                                   "SJM"
  "solomon islands"                                          "SLB"
  "sierra leone"                                             "SLE"
  "el salvador"                                              "SLV"
  "san marino"                                               "SMR"
  "somalia"                                                  "SOM"
  "saint pierre and miquelon"                                "SPM"
  "serbia"                                                   "SRB"
  "south sudan"                                              "SSD"
  "sao tome and principe"                                    "STP"
  "suriname"                                                 "SUR"
  "slovakia"                                                 "SVK"
  "slovenia"                                                 "SVN"
  "sweden"                                                   "SWE"
  "eswatini"                                                 "SWZ"
  "swaziland"                                                "SWZ"
  "sint maarten"                                             "SXM"
  "kingdom of eswatini"                                      "SWZ"
  "seychelles"                                               "SYC"
  "syria"                                                    "SYR"
  "syrian arab republic"                                     "SYR"
  "turks and caicos islands"                                 "TCA"
  "the turks and caicos islands"                             "TCA"
  "chad"                                                     "TCD"
  "togo"                                                     "TGO"
  "thailand"                                                 "THA"
  "tajikistan"                                               "TJK"
  "tokelau"                                                  "TKL"
  "turkmenistan"                                             "TKM"
  "timor-leste"                                              "TLS"
  "timor leste"                                              "TLS"
  "tonga"                                                    "TON"
  "trinidad and tobago"                                      "TTO"
  "tunisia"                                                  "TUN"
  "turkey"                                                   "TUR"
  "tuvalu"                                                   "TUV"
  "taiwan"                                                   "TWN"
  "taiwan province of china"                                 "TWN"
  "taiwan (province of china)"                               "TWN"
  "tanzania"                                                 "TZA"
  "united republic of tanzania"                              "TZA"
  "the united republic of tanzania"                          "TZA"
  "uganda"                                                   "UGA"
  "ukraine"                                                  "UKR"
  "united states minor outlying islands"                     "UMI"
  "the united states minor outlying islands"                 "UMI"
  "uruguay"                                                  "URY"
  "usa"                                                      "USA"
  "united states"                                            "USA"
  "united states of america"                                 "USA"
  "the united states of america"                             "USA"
  "uzbekistan"                                               "UZB"
  "vatican"                                                  "VAT"
  "the vatican"                                              "VAT"
  "vatican city"                                             "VAT"
  "holy see"                                                 "VAT"
  "the holy see"                                             "VAT"
  "saint vincent and the grenadines"                         "VCT"
  "venezuela"                                                "VEN"
  "bolivarian republic of venezuela"                         "VEN"
  "british virgin islands"                                   "VGB"
  "us virgin islands"                                        "VIR"
  "u.s. virgin islands"                                      "VIR"
  "vietnam"                                                  "VNM"
  "viet nam"                                                 "VNM"
  "vanuatu"                                                  "VUT"
  "wallis and futuna"                                        "WLF"
  "samoa"                                                    "WSM"
  "yemen"                                                    "YEM"
  "south africa"                                             "ZAF"
  "republic of south africa"                                 "ZAF"
  "zambia"                                                   "ZMB"
  "zimbabwe"                                                 "ZWE"

  ; Unofficial 3 letter codes
  "europe"                                                   "EUR"
  "england"                                                  "ENG"
  "scotland"                                                 "SCO"
  "wales"                                                    "WAL"
  })

(defn name-to-alpha-3
  "Attempts to determine the ISO-3166-alpha-3 code for the given long-form name. Returns nil if no matching code was found."
  [name]
  (when-not (s/blank? name)
    (get name-to-alpha-3-map (s/lower-case (s/trim name)))))

(def alpha-3-to-alpha-2 {
  "ABW" "AW"          ; Aruba
  "AFG" "AF"          ; Afghanistan
  "AGO" "AO"          ; Angola
  "AIA" "AI"          ; Anguilla
  "ALA" "AX"          ; Åland Islands
  "ALB" "AL"          ; Albania
  "AND" "AD"          ; Andorra
  "ARE" "AE"          ; United Arab Emirates (the)
  "ARG" "AR"          ; Argentina
  "ARM" "AM"          ; Armenia
  "ASM" "AS"          ; American Samoa
  "ATA" "AQ"          ; Antarctica
  "ATF" "TF"          ; French Southern Territories (the)
  "ATG" "AG"          ; Antigua and Barbuda
  "AUS" "AU"          ; Australia
  "AUT" "AT"          ; Austria
  "AZE" "AZ"          ; Azerbaijan
  "BDI" "BI"          ; Burundi
  "BEL" "BE"          ; Belgium
  "BEN" "BJ"          ; Benin
  "BES" "BQ"          ; Bonaire, Sint Eustatius and Saba
  "BFA" "BF"          ; Burkina Faso
  "BGD" "BD"          ; Bangladesh
  "BGR" "BG"          ; Bulgaria
  "BHR" "BH"          ; Bahrain
  "BHS" "BS"          ; Bahamas (the)
  "BIH" "BA"          ; Bosnia and Herzegovina
  "BLM" "BL"          ; Saint Barthélemy
  "BLR" "BY"          ; Belarus
  "BLZ" "BZ"          ; Belize
  "BMU" "BM"          ; Bermuda
  "BOL" "BO"          ; Bolivia (Plurinational State of)
  "BRA" "BR"          ; Brazil
  "BRB" "BB"          ; Barbados
  "BRN" "BN"          ; Brunei Darussalam
  "BTN" "BT"          ; Bhutan
  "BVT" "BV"          ; Bouvet Island
  "BWA" "BW"          ; Botswana
  "CAF" "CF"          ; Central African Republic (the)
  "CAN" "CA"          ; Canada
  "CCK" "CC"          ; Cocos (Keeling) Islands (the)
  "CHE" "CH"          ; Switzerland
  "CHL" "CL"          ; Chile
  "CHN" "CN"          ; China
  "CIV" "CI"          ; Côte d'Ivoire
  "CMR" "CM"          ; Cameroon
  "COD" "CD"          ; Congo (the Democratic Republic of the)
  "COG" "CG"          ; Congo (the)
  "COK" "CK"          ; Cook Islands (the)
  "COL" "CO"          ; Colombia
  "COM" "KM"          ; Comoros (the)
  "CPV" "CV"          ; Cabo Verde
  "CRI" "CR"          ; Costa Rica
  "CUB" "CU"          ; Cuba
  "CUW" "CW"          ; Curaçao
  "CXR" "CX"          ; Christmas Island
  "CYM" "KY"          ; Cayman Islands (the)
  "CYP" "CY"          ; Cyprus
  "CZE" "CZ"          ; Czechia
  "DEU" "DE"          ; Germany
  "DJI" "DJ"          ; Djibouti
  "DMA" "DM"          ; Dominica
  "DNK" "DK"          ; Denmark
  "DOM" "DO"          ; Dominican Republic (the)
  "DZA" "DZ"          ; Algeria
  "ECU" "EC"          ; Ecuador
  "EGY" "EG"          ; Egypt
  "ERI" "ER"          ; Eritrea
  "ESH" "EH"          ; Western Sahara
  "ESP" "ES"          ; Spain
  "EST" "EE"          ; Estonia
  "ETH" "ET"          ; Ethiopia
  "FIN" "FI"          ; Finland
  "FJI" "FJ"          ; Fiji
  "FLK" "FK"          ; Falkland Islands (the) [Malvinas]
  "FRA" "FR"          ; France
  "FRO" "FO"          ; Faroe Islands (the)
  "FSM" "FM"          ; Micronesia (Federated States of)
  "GAB" "GA"          ; Gabon
  "GBR" "GB"          ; United Kingdom of Great Britain and Northern Ireland (the)
  "GEO" "GE"          ; Georgia
  "GGY" "GG"          ; Guernsey
  "GHA" "GH"          ; Ghana
  "GIB" "GI"          ; Gibraltar
  "GIN" "GN"          ; Guinea
  "GLP" "GP"          ; Guadeloupe
  "GMB" "GM"          ; Gambia (the)
  "GNB" "GW"          ; Guinea-Bissau
  "GNQ" "GQ"          ; Equatorial Guinea
  "GRC" "GR"          ; Greece
  "GRD" "GD"          ; Grenada
  "GRL" "GL"          ; Greenland
  "GTM" "GT"          ; Guatemala
  "GUF" "GF"          ; French Guiana
  "GUM" "GU"          ; Guam
  "GUY" "GY"          ; Guyana
  "HKG" "HK"          ; Hong Kong
  "HMD" "HM"          ; Heard Island and McDonald Islands
  "HND" "HN"          ; Honduras
  "HRV" "HR"          ; Croatia
  "HTI" "HT"          ; Haiti
  "HUN" "HU"          ; Hungary
  "IDN" "ID"          ; Indonesia
  "IMN" "IM"          ; Isle of Man
  "IND" "IN"          ; India
  "IOT" "IO"          ; British Indian Ocean Territory (the)
  "IRL" "IE"          ; Ireland
  "IRN" "IR"          ; Iran (Islamic Republic of)
  "IRQ" "IQ"          ; Iraq
  "ISL" "IS"          ; Iceland
  "ISR" "IL"          ; Israel
  "ITA" "IT"          ; Italy
  "JAM" "JM"          ; Jamaica
  "JEY" "JE"          ; Jersey
  "JOR" "JO"          ; Jordan
  "JPN" "JP"          ; Japan
  "KAZ" "KZ"          ; Kazakhstan
  "KEN" "KE"          ; Kenya
  "KGZ" "KG"          ; Kyrgyzstan
  "KHM" "KH"          ; Cambodia
  "KIR" "KI"          ; Kiribati
  "KNA" "KN"          ; Saint Kitts and Nevis
  "KOR" "KR"          ; Korea (the Republic of)
  "KWT" "KW"          ; Kuwait
  "LAO" "LA"          ; Lao People's Democratic Republic (the)
  "LBN" "LB"          ; Lebanon
  "LBR" "LR"          ; Liberia
  "LBY" "LY"          ; Libya
  "LCA" "LC"          ; Saint Lucia
  "LIE" "LI"          ; Liechtenstein
  "LKA" "LK"          ; Sri Lanka
  "LSO" "LS"          ; Lesotho
  "LTU" "LT"          ; Lithuania
  "LUX" "LU"          ; Luxembourg
  "LVA" "LV"          ; Latvia
  "MAC" "MO"          ; Macao
  "MAF" "MF"          ; Saint Martin (French part)
  "MAR" "MA"          ; Morocco
  "MCO" "MC"          ; Monaco
  "MDA" "MD"          ; Moldova (the Republic of)
  "MDG" "MG"          ; Madagascar
  "MDV" "MV"          ; Maldives
  "MEX" "MX"          ; Mexico
  "MHL" "MH"          ; Marshall Islands (the)
  "MKD" "MK"          ; Republic of North Macedonia
  "MLI" "ML"          ; Mali
  "MLT" "MT"          ; Malta
  "MMR" "MM"          ; Myanmar
  "MNE" "ME"          ; Montenegro
  "MNG" "MN"          ; Mongolia
  "MNP" "MP"          ; Northern Mariana Islands (the)
  "MOZ" "MZ"          ; Mozambique
  "MRT" "MR"          ; Mauritania
  "MSR" "MS"          ; Montserrat
  "MTQ" "MQ"          ; Martinique
  "MUS" "MU"          ; Mauritius
  "MWI" "MW"          ; Malawi
  "MYS" "MY"          ; Malaysia
  "MYT" "YT"          ; Mayotte
  "NAM" "NA"          ; Namibia
  "NCL" "NC"          ; New Caledonia
  "NER" "NE"          ; Niger (the)
  "NFK" "NF"          ; Norfolk Island
  "NGA" "NG"          ; Nigeria
  "NIC" "NI"          ; Nicaragua
  "NIU" "NU"          ; Niue
  "NLD" "NL"          ; Netherlands (the)
  "NOR" "NO"          ; Norway
  "NPL" "NP"          ; Nepal
  "NRU" "NR"          ; Nauru
  "NZL" "NZ"          ; New Zealand
  "OMN" "OM"          ; Oman
  "PAK" "PK"          ; Pakistan
  "PAN" "PA"          ; Panama
  "PCN" "PN"          ; Pitcairn
  "PER" "PE"          ; Peru
  "PHL" "PH"          ; Philippines (the)
  "PLW" "PW"          ; Palau
  "PNG" "PG"          ; Papua New Guinea
  "POL" "PL"          ; Poland
  "PRI" "PR"          ; Puerto Rico
  "PRK" "KP"          ; Korea (the Democratic People's Republic of)
  "PRT" "PT"          ; Portugal
  "PRY" "PY"          ; Paraguay
  "PSE" "PS"          ; Palestine, State of
  "PYF" "PF"          ; French Polynesia
  "QAT" "QA"          ; Qatar
  "REU" "RE"          ; Réunion
  "ROU" "RO"          ; Romania
  "RUS" "RU"          ; Russian Federation (the)
  "RWA" "RW"          ; Rwanda
  "SAU" "SA"          ; Saudi Arabia
  "SDN" "SD"          ; Sudan (the)
  "SEN" "SN"          ; Senegal
  "SGP" "SG"          ; Singapore
  "SGS" "GS"          ; South Georgia and the South Sandwich Islands
  "SHN" "SH"          ; Saint Helena, Ascension and Tristan da Cunha
  "SJM" "SJ"          ; Svalbard and Jan Mayen
  "SLB" "SB"          ; Solomon Islands
  "SLE" "SL"          ; Sierra Leone
  "SLV" "SV"          ; El Salvador
  "SMR" "SM"          ; San Marino
  "SOM" "SO"          ; Somalia
  "SPM" "PM"          ; Saint Pierre and Miquelon
  "SRB" "RS"          ; Serbia
  "SSD" "SS"          ; South Sudan
  "STP" "ST"          ; Sao Tome and Principe
  "SUR" "SR"          ; Suriname
  "SVK" "SK"          ; Slovakia
  "SVN" "SI"          ; Slovenia
  "SWE" "SE"          ; Sweden
  "SWZ" "SZ"          ; Eswatini
  "SXM" "SX"          ; Sint Maarten (Dutch part)
  "SYC" "SC"          ; Seychelles
  "SYR" "SY"          ; Syrian Arab Republic
  "TCA" "TC"          ; Turks and Caicos Islands (the)
  "TCD" "TD"          ; Chad
  "TGO" "TG"          ; Togo
  "THA" "TH"          ; Thailand
  "TJK" "TJ"          ; Tajikistan
  "TKL" "TK"          ; Tokelau
  "TKM" "TM"          ; Turkmenistan
  "TLS" "TL"          ; Timor-Leste
  "TON" "TO"          ; Tonga
  "TTO" "TT"          ; Trinidad and Tobago
  "TUN" "TN"          ; Tunisia
  "TUR" "TR"          ; Turkey
  "TUV" "TV"          ; Tuvalu
  "TWN" "TW"          ; Taiwan (Province of China)
  "TZA" "TZ"          ; Tanzania, United Republic of
  "UGA" "UG"          ; Uganda
  "UKR" "UA"          ; Ukraine
  "UMI" "UM"          ; United States Minor Outlying Islands (the)
  "URY" "UY"          ; Uruguay
  "USA" "US"          ; United States of America (the)
  "UZB" "UZ"          ; Uzbekistan
  "VAT" "VA"          ; Holy See (the)
  "VCT" "VC"          ; Saint Vincent and the Grenadines
  "VEN" "VE"          ; Venezuela (Bolivarian Republic of)
  "VGB" "VG"          ; Virgin Islands (British)
  "VIR" "VI"          ; Virgin Islands (U.S.)
  "VNM" "VN"          ; Viet Nam
  "VUT" "VU"          ; Vanuatu
  "WLF" "WF"          ; Wallis and Futuna
  "WSM" "WS"          ; Samoa
  "YEM" "YE"          ; Yemen
  "ZAF" "ZA"          ; South Africa
  "ZMB" "ZM"          ; Zambia
  "ZWE" "ZW"          ; Zimbabwe

  ; Unofficial 3 letter codes (and their nearest, non-standard 2 letter alternatives)
  "EUR" "EU"          ; Europe
  "RSA" "ZA"          ; South Africa (alternative code)
  "ENG" "EN"          ; England
  "SCO" "SC󠁳󠁣󠁴󠁿"          ; Scotland
  "WAL" "WL"          ; Wales
  })

(def alpha-2-to-alpha-3 (set/map-invert alpha-3-to-alpha-2))
