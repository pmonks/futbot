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

(ns futbot.flags
  (:require [clojure.string        :as s]
            [clojure.tools.logging :as log]))

(defn emoji-flag
  "Returns the emoji flag for a valid ISO-3166-1 alpha-2 country code (e.g. 'AU', 'US', etc.), or another Unicode character for invalid country codes."
  [iso-3166-1-alpha-2]
  (let [code-point-offset 127397
        country-code      (if (= "UK" (s/upper-case iso-3166-1-alpha-2))
                            "GB"
                            (s/upper-case iso-3166-1-alpha-2))]
      (str (doto (StringBuilder.)
                 (.appendCodePoint (+ code-point-offset (int (first country-code))))
                 (.appendCodePoint (+ code-point-offset (int (second country-code))))))))

(def alpha-3-to-flag {
  "ABW" (emoji-flag "AW")          ; Aruba
  "AFG" (emoji-flag "AF")          ; Afghanistan
  "AGO" (emoji-flag "AO")          ; Angola
  "AIA" (emoji-flag "AI")          ; Anguilla
  "ALA" (emoji-flag "AX")          ; Åland Islands
  "ALB" (emoji-flag "AL")          ; Albania
  "AND" (emoji-flag "AD")          ; Andorra
  "ARE" (emoji-flag "AE")          ; United Arab Emirates (the)
  "ARG" (emoji-flag "AR")          ; Argentina
  "ARM" (emoji-flag "AM")          ; Armenia
  "ASM" (emoji-flag "AS")          ; American Samoa
  "ATA" (emoji-flag "AQ")          ; Antarctica
  "ATF" (emoji-flag "TF")          ; French Southern Territories (the)
  "ATG" (emoji-flag "AG")          ; Antigua and Barbuda
  "AUS" (emoji-flag "AU")          ; Australia
  "AUT" (emoji-flag "AT")          ; Austria
  "AZE" (emoji-flag "AZ")          ; Azerbaijan
  "BDI" (emoji-flag "BI")          ; Burundi
  "BEL" (emoji-flag "BE")          ; Belgium
  "BEN" (emoji-flag "BJ")          ; Benin
  "BES" (emoji-flag "BQ")          ; Bonaire, Sint Eustatius and Saba
  "BFA" (emoji-flag "BF")          ; Burkina Faso
  "BGD" (emoji-flag "BD")          ; Bangladesh
  "BGR" (emoji-flag "BG")          ; Bulgaria
  "BHR" (emoji-flag "BH")          ; Bahrain
  "BHS" (emoji-flag "BS")          ; Bahamas (the)
  "BIH" (emoji-flag "BA")          ; Bosnia and Herzegovina
  "BLM" (emoji-flag "BL")          ; Saint Barthélemy
  "BLR" (emoji-flag "BY")          ; Belarus
  "BLZ" (emoji-flag "BZ")          ; Belize
  "BMU" (emoji-flag "BM")          ; Bermuda
  "BOL" (emoji-flag "BO")          ; Bolivia (Plurinational State of)
  "BRA" (emoji-flag "BR")          ; Brazil
  "BRB" (emoji-flag "BB")          ; Barbados
  "BRN" (emoji-flag "BN")          ; Brunei Darussalam
  "BTN" (emoji-flag "BT")          ; Bhutan
  "BVT" (emoji-flag "BV")          ; Bouvet Island
  "BWA" (emoji-flag "BW")          ; Botswana
  "CAF" (emoji-flag "CF")          ; Central African Republic (the)
  "CAN" (emoji-flag "CA")          ; Canada
  "CCK" (emoji-flag "CC")          ; Cocos (Keeling) Islands (the)
  "CHE" (emoji-flag "CH")          ; Switzerland
  "CHL" (emoji-flag "CL")          ; Chile
  "CHN" (emoji-flag "CN")          ; China
  "CIV" (emoji-flag "CI")          ; Côte d'Ivoire
  "CMR" (emoji-flag "CM")          ; Cameroon
  "COD" (emoji-flag "CD")          ; Congo (the Democratic Republic of the)
  "COG" (emoji-flag "CG")          ; Congo (the)
  "COK" (emoji-flag "CK")          ; Cook Islands (the)
  "COL" (emoji-flag "CO")          ; Colombia
  "COM" (emoji-flag "KM")          ; Comoros (the)
  "CPV" (emoji-flag "CV")          ; Cabo Verde
  "CRI" (emoji-flag "CR")          ; Costa Rica
  "CUB" (emoji-flag "CU")          ; Cuba
  "CUW" (emoji-flag "CW")          ; Curaçao
  "CXR" (emoji-flag "CX")          ; Christmas Island
  "CYM" (emoji-flag "KY")          ; Cayman Islands (the)
  "CYP" (emoji-flag "CY")          ; Cyprus
  "CZE" (emoji-flag "CZ")          ; Czechia
  "DEU" (emoji-flag "DE")          ; Germany
  "DJI" (emoji-flag "DJ")          ; Djibouti
  "DMA" (emoji-flag "DM")          ; Dominica
  "DNK" (emoji-flag "DK")          ; Denmark
  "DOM" (emoji-flag "DO")          ; Dominican Republic (the)
  "DZA" (emoji-flag "DZ")          ; Algeria
  "ECU" (emoji-flag "EC")          ; Ecuador
  "EGY" (emoji-flag "EG")          ; Egypt
  "ERI" (emoji-flag "ER")          ; Eritrea
  "ESH" (emoji-flag "EH")          ; Western Sahara
  "ESP" (emoji-flag "ES")          ; Spain
  "EST" (emoji-flag "EE")          ; Estonia
  "ETH" (emoji-flag "ET")          ; Ethiopia
  "FIN" (emoji-flag "FI")          ; Finland
  "FJI" (emoji-flag "FJ")          ; Fiji
  "FLK" (emoji-flag "FK")          ; Falkland Islands (the) [Malvinas]
  "FRA" (emoji-flag "FR")          ; France
  "FRO" (emoji-flag "FO")          ; Faroe Islands (the)
  "FSM" (emoji-flag "FM")          ; Micronesia (Federated States of)
  "GAB" (emoji-flag "GA")          ; Gabon
  "GBR" (emoji-flag "GB")          ; United Kingdom of Great Britain and Northern Ireland (the)
  "GEO" (emoji-flag "GE")          ; Georgia
  "GGY" (emoji-flag "GG")          ; Guernsey
  "GHA" (emoji-flag "GH")          ; Ghana
  "GIB" (emoji-flag "GI")          ; Gibraltar
  "GIN" (emoji-flag "GN")          ; Guinea
  "GLP" (emoji-flag "GP")          ; Guadeloupe
  "GMB" (emoji-flag "GM")          ; Gambia (the)
  "GNB" (emoji-flag "GW")          ; Guinea-Bissau
  "GNQ" (emoji-flag "GQ")          ; Equatorial Guinea
  "GRC" (emoji-flag "GR")          ; Greece
  "GRD" (emoji-flag "GD")          ; Grenada
  "GRL" (emoji-flag "GL")          ; Greenland
  "GTM" (emoji-flag "GT")          ; Guatemala
  "GUF" (emoji-flag "GF")          ; French Guiana
  "GUM" (emoji-flag "GU")          ; Guam
  "GUY" (emoji-flag "GY")          ; Guyana
  "HKG" (emoji-flag "HK")          ; Hong Kong
  "HMD" (emoji-flag "HM")          ; Heard Island and McDonald Islands
  "HND" (emoji-flag "HN")          ; Honduras
  "HRV" (emoji-flag "HR")          ; Croatia
  "HTI" (emoji-flag "HT")          ; Haiti
  "HUN" (emoji-flag "HU")          ; Hungary
  "IDN" (emoji-flag "ID")          ; Indonesia
  "IMN" (emoji-flag "IM")          ; Isle of Man
  "IND" (emoji-flag "IN")          ; India
  "IOT" (emoji-flag "IO")          ; British Indian Ocean Territory (the)
  "IRL" (emoji-flag "IE")          ; Ireland
  "IRN" (emoji-flag "IR")          ; Iran (Islamic Republic of)
  "IRQ" (emoji-flag "IQ")          ; Iraq
  "ISL" (emoji-flag "IS")          ; Iceland
  "ISR" (emoji-flag "IL")          ; Israel
  "ITA" (emoji-flag "IT")          ; Italy
  "JAM" (emoji-flag "JM")          ; Jamaica
  "JEY" (emoji-flag "JE")          ; Jersey
  "JOR" (emoji-flag "JO")          ; Jordan
  "JPN" (emoji-flag "JP")          ; Japan
  "KAZ" (emoji-flag "KZ")          ; Kazakhstan
  "KEN" (emoji-flag "KE")          ; Kenya
  "KGZ" (emoji-flag "KG")          ; Kyrgyzstan
  "KHM" (emoji-flag "KH")          ; Cambodia
  "KIR" (emoji-flag "KI")          ; Kiribati
  "KNA" (emoji-flag "KN")          ; Saint Kitts and Nevis
  "KOR" (emoji-flag "KR")          ; Korea (the Republic of)
  "KWT" (emoji-flag "KW")          ; Kuwait
  "LAO" (emoji-flag "LA")          ; Lao People's Democratic Republic (the)
  "LBN" (emoji-flag "LB")          ; Lebanon
  "LBR" (emoji-flag "LR")          ; Liberia
  "LBY" (emoji-flag "LY")          ; Libya
  "LCA" (emoji-flag "LC")          ; Saint Lucia
  "LIE" (emoji-flag "LI")          ; Liechtenstein
  "LKA" (emoji-flag "LK")          ; Sri Lanka
  "LSO" (emoji-flag "LS")          ; Lesotho
  "LTU" (emoji-flag "LT")          ; Lithuania
  "LUX" (emoji-flag "LU")          ; Luxembourg
  "LVA" (emoji-flag "LV")          ; Latvia
  "MAC" (emoji-flag "MO")          ; Macao
  "MAF" (emoji-flag "MF")          ; Saint Martin (French part)
  "MAR" (emoji-flag "MA")          ; Morocco
  "MCO" (emoji-flag "MC")          ; Monaco
  "MDA" (emoji-flag "MD")          ; Moldova (the Republic of)
  "MDG" (emoji-flag "MG")          ; Madagascar
  "MDV" (emoji-flag "MV")          ; Maldives
  "MEX" (emoji-flag "MX")          ; Mexico
  "MHL" (emoji-flag "MH")          ; Marshall Islands (the)
  "MKD" (emoji-flag "MK")          ; Republic of North Macedonia
  "MLI" (emoji-flag "ML")          ; Mali
  "MLT" (emoji-flag "MT")          ; Malta
  "MMR" (emoji-flag "MM")          ; Myanmar
  "MNE" (emoji-flag "ME")          ; Montenegro
  "MNG" (emoji-flag "MN")          ; Mongolia
  "MNP" (emoji-flag "MP")          ; Northern Mariana Islands (the)
  "MOZ" (emoji-flag "MZ")          ; Mozambique
  "MRT" (emoji-flag "MR")          ; Mauritania
  "MSR" (emoji-flag "MS")          ; Montserrat
  "MTQ" (emoji-flag "MQ")          ; Martinique
  "MUS" (emoji-flag "MU")          ; Mauritius
  "MWI" (emoji-flag "MW")          ; Malawi
  "MYS" (emoji-flag "MY")          ; Malaysia
  "MYT" (emoji-flag "YT")          ; Mayotte
  "NAM" (emoji-flag "NA")          ; Namibia
  "NCL" (emoji-flag "NC")          ; New Caledonia
  "NER" (emoji-flag "NE")          ; Niger (the)
  "NFK" (emoji-flag "NF")          ; Norfolk Island
  "NGA" (emoji-flag "NG")          ; Nigeria
  "NIC" (emoji-flag "NI")          ; Nicaragua
  "NIU" (emoji-flag "NU")          ; Niue
  "NLD" (emoji-flag "NL")          ; Netherlands (the)
  "NOR" (emoji-flag "NO")          ; Norway
  "NPL" (emoji-flag "NP")          ; Nepal
  "NRU" (emoji-flag "NR")          ; Nauru
  "NZL" (emoji-flag "NZ")          ; New Zealand
  "OMN" (emoji-flag "OM")          ; Oman
  "PAK" (emoji-flag "PK")          ; Pakistan
  "PAN" (emoji-flag "PA")          ; Panama
  "PCN" (emoji-flag "PN")          ; Pitcairn
  "PER" (emoji-flag "PE")          ; Peru
  "PHL" (emoji-flag "PH")          ; Philippines (the)
  "PLW" (emoji-flag "PW")          ; Palau
  "PNG" (emoji-flag "PG")          ; Papua New Guinea
  "POL" (emoji-flag "PL")          ; Poland
  "PRI" (emoji-flag "PR")          ; Puerto Rico
  "PRK" (emoji-flag "KP")          ; Korea (the Democratic People's Republic of)
  "PRT" (emoji-flag "PT")          ; Portugal
  "PRY" (emoji-flag "PY")          ; Paraguay
  "PSE" (emoji-flag "PS")          ; Palestine, State of
  "PYF" (emoji-flag "PF")          ; French Polynesia
  "QAT" (emoji-flag "QA")          ; Qatar
  "REU" (emoji-flag "RE")          ; Réunion
  "ROU" (emoji-flag "RO")          ; Romania
  "RUS" (emoji-flag "RU")          ; Russian Federation (the)
  "RWA" (emoji-flag "RW")          ; Rwanda
  "SAU" (emoji-flag "SA")          ; Saudi Arabia
  "SDN" (emoji-flag "SD")          ; Sudan (the)
  "SEN" (emoji-flag "SN")          ; Senegal
  "SGP" (emoji-flag "SG")          ; Singapore
  "SGS" (emoji-flag "GS")          ; South Georgia and the South Sandwich Islands
  "SHN" (emoji-flag "SH")          ; Saint Helena, Ascension and Tristan da Cunha
  "SJM" (emoji-flag "SJ")          ; Svalbard and Jan Mayen
  "SLB" (emoji-flag "SB")          ; Solomon Islands
  "SLE" (emoji-flag "SL")          ; Sierra Leone
  "SLV" (emoji-flag "SV")          ; El Salvador
  "SMR" (emoji-flag "SM")          ; San Marino
  "SOM" (emoji-flag "SO")          ; Somalia
  "SPM" (emoji-flag "PM")          ; Saint Pierre and Miquelon
  "SRB" (emoji-flag "RS")          ; Serbia
  "SSD" (emoji-flag "SS")          ; South Sudan
  "STP" (emoji-flag "ST")          ; Sao Tome and Principe
  "SUR" (emoji-flag "SR")          ; Suriname
  "SVK" (emoji-flag "SK")          ; Slovakia
  "SVN" (emoji-flag "SI")          ; Slovenia
  "SWE" (emoji-flag "SE")          ; Sweden
  "SWZ" (emoji-flag "SZ")          ; Eswatini
  "SXM" (emoji-flag "SX")          ; Sint Maarten (Dutch part)
  "SYC" (emoji-flag "SC")          ; Seychelles
  "SYR" (emoji-flag "SY")          ; Syrian Arab Republic
  "TCA" (emoji-flag "TC")          ; Turks and Caicos Islands (the)
  "TCD" (emoji-flag "TD")          ; Chad
  "TGO" (emoji-flag "TG")          ; Togo
  "THA" (emoji-flag "TH")          ; Thailand
  "TJK" (emoji-flag "TJ")          ; Tajikistan
  "TKL" (emoji-flag "TK")          ; Tokelau
  "TKM" (emoji-flag "TM")          ; Turkmenistan
  "TLS" (emoji-flag "TL")          ; Timor-Leste
  "TON" (emoji-flag "TO")          ; Tonga
  "TTO" (emoji-flag "TT")          ; Trinidad and Tobago
  "TUN" (emoji-flag "TN")          ; Tunisia
  "TUR" (emoji-flag "TR")          ; Turkey
  "TUV" (emoji-flag "TV")          ; Tuvalu
  "TWN" (emoji-flag "TW")          ; Taiwan (Province of China)
  "TZA" (emoji-flag "TZ")          ; Tanzania, United Republic of
  "UGA" (emoji-flag "UG")          ; Uganda
  "UKR" (emoji-flag "UA")          ; Ukraine
  "UMI" (emoji-flag "UM")          ; United States Minor Outlying Islands (the)
  "URY" (emoji-flag "UY")          ; Uruguay
  "USA" (emoji-flag "US")          ; United States of America (the)
  "UZB" (emoji-flag "UZ")          ; Uzbekistan
  "VAT" (emoji-flag "VA")          ; Holy See (the)
  "VCT" (emoji-flag "VC")          ; Saint Vincent and the Grenadines
  "VEN" (emoji-flag "VE")          ; Venezuela (Bolivarian Republic of)
  "VGB" (emoji-flag "VG")          ; Virgin Islands (British)
  "VIR" (emoji-flag "VI")          ; Virgin Islands (U.S.)
  "VNM" (emoji-flag "VN")          ; Viet Nam
  "VUT" (emoji-flag "VU")          ; Vanuatu
  "WLF" (emoji-flag "WF")          ; Wallis and Futuna
  "WSM" (emoji-flag "WS")          ; Samoa
  "YEM" (emoji-flag "YE")          ; Yemen
  "ZAF" (emoji-flag "ZA")          ; South Africa
  "ZMB" (emoji-flag "ZM")          ; Zambia
  "ZWE" (emoji-flag "ZW")          ; Zimbabwe

  ; 'bonus' 3 letter codes, including some returned by football-data.org
  "EUR" (emoji-flag "EU")          ; Europe
  "INT" "🗺️"                       ; World - could also use (emoji-flag "UN")
  "AFR" "🌍"                       ; Africa
  "RSA" (emoji-flag "ZA")          ; South Africa
  "ENG" "🏴󠁧󠁢󠁥󠁮󠁧󠁿"                       ; England
  "SCO" "🏴󠁧󠁢󠁳󠁣󠁴󠁿"                       ; Scotland
  "WAL" "🏴󠁧󠁢󠁷󠁬󠁳󠁿"                       ; Wales
  "BLK" "🏴"                       ; Plain black flag
  "WHT" "🏳"                       ; Plain white flag
  "GAY" "🏳️‍🌈"                       ; Pride flag
  "TRN" "🏳️‍⚧️"                       ; Transgender flag
  "CHK" "🏁"                       ; Checkered flag
  "TRI" "🚩"                       ; Triangular flag
  "CRX" "🎌"                       ; Crossed flags
  "PIR" "🏴‍☠️"                       ; Pirate flag
  })

(defn flag
  "Returns the emoji flag for the given country-code (an ISO-3166-1 alpha-2 or alpha-3 code, or one of the 'bonus' 3 letter codes used by football-data.org).  Returns nil for invalid 3-letter codes, or another, non-flag Unicode character for an invalid 2-letter code."
  [country-code]
  (if country-code
    (let [code (s/upper-case (s/trim country-code))]
      (case (count code)
        2 (emoji-flag code)
        3 (get alpha-3-to-flag code nil)
        nil))))
