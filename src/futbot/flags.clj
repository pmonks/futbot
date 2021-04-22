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
  (:require [clojure.string  :as s]))

(defn alpha-2-to-flag
  "Returns the emoji flag (e.g. '🇦🇺', '🇺🇸') for a valid ISO-3166-1 alpha-2 country code (e.g. 'AU', 'US', etc.), or another Unicode character for invalid country codes."
  [iso-3166-1-alpha-2]
  (let [code-point-offset 127397
        country-code      (if (= "UK" (s/upper-case iso-3166-1-alpha-2))
                            "GB"
                            (s/upper-case iso-3166-1-alpha-2))]
      (str (doto (StringBuilder.)
                 (.appendCodePoint (+ code-point-offset (int (first country-code))))
                 (.appendCodePoint (+ code-point-offset (int (second country-code))))))))

(def alpha-3-to-flag {
  "ABW" (alpha-2-to-flag "AW")          ; Aruba
  "AFG" (alpha-2-to-flag "AF")          ; Afghanistan
  "AGO" (alpha-2-to-flag "AO")          ; Angola
  "AIA" (alpha-2-to-flag "AI")          ; Anguilla
  "ALA" (alpha-2-to-flag "AX")          ; Åland Islands
  "ALB" (alpha-2-to-flag "AL")          ; Albania
  "AND" (alpha-2-to-flag "AD")          ; Andorra
  "ARE" (alpha-2-to-flag "AE")          ; United Arab Emirates (the)
  "ARG" (alpha-2-to-flag "AR")          ; Argentina
  "ARM" (alpha-2-to-flag "AM")          ; Armenia
  "ASM" (alpha-2-to-flag "AS")          ; American Samoa
  "ATA" (alpha-2-to-flag "AQ")          ; Antarctica
  "ATF" (alpha-2-to-flag "TF")          ; French Southern Territories (the)
  "ATG" (alpha-2-to-flag "AG")          ; Antigua and Barbuda
  "AUS" (alpha-2-to-flag "AU")          ; Australia
  "AUT" (alpha-2-to-flag "AT")          ; Austria
  "AZE" (alpha-2-to-flag "AZ")          ; Azerbaijan
  "BDI" (alpha-2-to-flag "BI")          ; Burundi
  "BEL" (alpha-2-to-flag "BE")          ; Belgium
  "BEN" (alpha-2-to-flag "BJ")          ; Benin
  "BES" (alpha-2-to-flag "BQ")          ; Bonaire, Sint Eustatius and Saba
  "BFA" (alpha-2-to-flag "BF")          ; Burkina Faso
  "BGD" (alpha-2-to-flag "BD")          ; Bangladesh
  "BGR" (alpha-2-to-flag "BG")          ; Bulgaria
  "BHR" (alpha-2-to-flag "BH")          ; Bahrain
  "BHS" (alpha-2-to-flag "BS")          ; Bahamas (the)
  "BIH" (alpha-2-to-flag "BA")          ; Bosnia and Herzegovina
  "BLM" (alpha-2-to-flag "BL")          ; Saint Barthélemy
  "BLR" (alpha-2-to-flag "BY")          ; Belarus
  "BLZ" (alpha-2-to-flag "BZ")          ; Belize
  "BMU" (alpha-2-to-flag "BM")          ; Bermuda
  "BOL" (alpha-2-to-flag "BO")          ; Bolivia (Plurinational State of)
  "BRA" (alpha-2-to-flag "BR")          ; Brazil
  "BRB" (alpha-2-to-flag "BB")          ; Barbados
  "BRN" (alpha-2-to-flag "BN")          ; Brunei Darussalam
  "BTN" (alpha-2-to-flag "BT")          ; Bhutan
  "BVT" (alpha-2-to-flag "BV")          ; Bouvet Island
  "BWA" (alpha-2-to-flag "BW")          ; Botswana
  "CAF" (alpha-2-to-flag "CF")          ; Central African Republic (the)
  "CAN" (alpha-2-to-flag "CA")          ; Canada
  "CCK" (alpha-2-to-flag "CC")          ; Cocos (Keeling) Islands (the)
  "CHE" (alpha-2-to-flag "CH")          ; Switzerland
  "CHL" (alpha-2-to-flag "CL")          ; Chile
  "CHN" (alpha-2-to-flag "CN")          ; China
  "CIV" (alpha-2-to-flag "CI")          ; Côte d'Ivoire
  "CMR" (alpha-2-to-flag "CM")          ; Cameroon
  "COD" (alpha-2-to-flag "CD")          ; Congo (the Democratic Republic of the)
  "COG" (alpha-2-to-flag "CG")          ; Congo (the)
  "COK" (alpha-2-to-flag "CK")          ; Cook Islands (the)
  "COL" (alpha-2-to-flag "CO")          ; Colombia
  "COM" (alpha-2-to-flag "KM")          ; Comoros (the)
  "CPV" (alpha-2-to-flag "CV")          ; Cabo Verde
  "CRI" (alpha-2-to-flag "CR")          ; Costa Rica
  "CUB" (alpha-2-to-flag "CU")          ; Cuba
  "CUW" (alpha-2-to-flag "CW")          ; Curaçao
  "CXR" (alpha-2-to-flag "CX")          ; Christmas Island
  "CYM" (alpha-2-to-flag "KY")          ; Cayman Islands (the)
  "CYP" (alpha-2-to-flag "CY")          ; Cyprus
  "CZE" (alpha-2-to-flag "CZ")          ; Czechia
  "DEU" (alpha-2-to-flag "DE")          ; Germany
  "DJI" (alpha-2-to-flag "DJ")          ; Djibouti
  "DMA" (alpha-2-to-flag "DM")          ; Dominica
  "DNK" (alpha-2-to-flag "DK")          ; Denmark
  "DOM" (alpha-2-to-flag "DO")          ; Dominican Republic (the)
  "DZA" (alpha-2-to-flag "DZ")          ; Algeria
  "ECU" (alpha-2-to-flag "EC")          ; Ecuador
  "EGY" (alpha-2-to-flag "EG")          ; Egypt
  "ERI" (alpha-2-to-flag "ER")          ; Eritrea
  "ESH" (alpha-2-to-flag "EH")          ; Western Sahara
  "ESP" (alpha-2-to-flag "ES")          ; Spain
  "EST" (alpha-2-to-flag "EE")          ; Estonia
  "ETH" (alpha-2-to-flag "ET")          ; Ethiopia
  "FIN" (alpha-2-to-flag "FI")          ; Finland
  "FJI" (alpha-2-to-flag "FJ")          ; Fiji
  "FLK" (alpha-2-to-flag "FK")          ; Falkland Islands (the) [Malvinas]
  "FRA" (alpha-2-to-flag "FR")          ; France
  "FRO" (alpha-2-to-flag "FO")          ; Faroe Islands (the)
  "FSM" (alpha-2-to-flag "FM")          ; Micronesia (Federated States of)
  "GAB" (alpha-2-to-flag "GA")          ; Gabon
  "GBR" (alpha-2-to-flag "GB")          ; United Kingdom of Great Britain and Northern Ireland (the)
  "GEO" (alpha-2-to-flag "GE")          ; Georgia
  "GGY" (alpha-2-to-flag "GG")          ; Guernsey
  "GHA" (alpha-2-to-flag "GH")          ; Ghana
  "GIB" (alpha-2-to-flag "GI")          ; Gibraltar
  "GIN" (alpha-2-to-flag "GN")          ; Guinea
  "GLP" (alpha-2-to-flag "GP")          ; Guadeloupe
  "GMB" (alpha-2-to-flag "GM")          ; Gambia (the)
  "GNB" (alpha-2-to-flag "GW")          ; Guinea-Bissau
  "GNQ" (alpha-2-to-flag "GQ")          ; Equatorial Guinea
  "GRC" (alpha-2-to-flag "GR")          ; Greece
  "GRD" (alpha-2-to-flag "GD")          ; Grenada
  "GRL" (alpha-2-to-flag "GL")          ; Greenland
  "GTM" (alpha-2-to-flag "GT")          ; Guatemala
  "GUF" (alpha-2-to-flag "GF")          ; French Guiana
  "GUM" (alpha-2-to-flag "GU")          ; Guam
  "GUY" (alpha-2-to-flag "GY")          ; Guyana
  "HKG" (alpha-2-to-flag "HK")          ; Hong Kong
  "HMD" (alpha-2-to-flag "HM")          ; Heard Island and McDonald Islands
  "HND" (alpha-2-to-flag "HN")          ; Honduras
  "HRV" (alpha-2-to-flag "HR")          ; Croatia
  "HTI" (alpha-2-to-flag "HT")          ; Haiti
  "HUN" (alpha-2-to-flag "HU")          ; Hungary
  "IDN" (alpha-2-to-flag "ID")          ; Indonesia
  "IMN" (alpha-2-to-flag "IM")          ; Isle of Man
  "IND" (alpha-2-to-flag "IN")          ; India
  "IOT" (alpha-2-to-flag "IO")          ; British Indian Ocean Territory (the)
  "IRL" (alpha-2-to-flag "IE")          ; Ireland
  "IRN" (alpha-2-to-flag "IR")          ; Iran (Islamic Republic of)
  "IRQ" (alpha-2-to-flag "IQ")          ; Iraq
  "ISL" (alpha-2-to-flag "IS")          ; Iceland
  "ISR" (alpha-2-to-flag "IL")          ; Israel
  "ITA" (alpha-2-to-flag "IT")          ; Italy
  "JAM" (alpha-2-to-flag "JM")          ; Jamaica
  "JEY" (alpha-2-to-flag "JE")          ; Jersey
  "JOR" (alpha-2-to-flag "JO")          ; Jordan
  "JPN" (alpha-2-to-flag "JP")          ; Japan
  "KAZ" (alpha-2-to-flag "KZ")          ; Kazakhstan
  "KEN" (alpha-2-to-flag "KE")          ; Kenya
  "KGZ" (alpha-2-to-flag "KG")          ; Kyrgyzstan
  "KHM" (alpha-2-to-flag "KH")          ; Cambodia
  "KIR" (alpha-2-to-flag "KI")          ; Kiribati
  "KNA" (alpha-2-to-flag "KN")          ; Saint Kitts and Nevis
  "KOR" (alpha-2-to-flag "KR")          ; Korea (the Republic of)
  "KWT" (alpha-2-to-flag "KW")          ; Kuwait
  "LAO" (alpha-2-to-flag "LA")          ; Lao People's Democratic Republic (the)
  "LBN" (alpha-2-to-flag "LB")          ; Lebanon
  "LBR" (alpha-2-to-flag "LR")          ; Liberia
  "LBY" (alpha-2-to-flag "LY")          ; Libya
  "LCA" (alpha-2-to-flag "LC")          ; Saint Lucia
  "LIE" (alpha-2-to-flag "LI")          ; Liechtenstein
  "LKA" (alpha-2-to-flag "LK")          ; Sri Lanka
  "LSO" (alpha-2-to-flag "LS")          ; Lesotho
  "LTU" (alpha-2-to-flag "LT")          ; Lithuania
  "LUX" (alpha-2-to-flag "LU")          ; Luxembourg
  "LVA" (alpha-2-to-flag "LV")          ; Latvia
  "MAC" (alpha-2-to-flag "MO")          ; Macao
  "MAF" (alpha-2-to-flag "MF")          ; Saint Martin (French part)
  "MAR" (alpha-2-to-flag "MA")          ; Morocco
  "MCO" (alpha-2-to-flag "MC")          ; Monaco
  "MDA" (alpha-2-to-flag "MD")          ; Moldova (the Republic of)
  "MDG" (alpha-2-to-flag "MG")          ; Madagascar
  "MDV" (alpha-2-to-flag "MV")          ; Maldives
  "MEX" (alpha-2-to-flag "MX")          ; Mexico
  "MHL" (alpha-2-to-flag "MH")          ; Marshall Islands (the)
  "MKD" (alpha-2-to-flag "MK")          ; Republic of North Macedonia
  "MLI" (alpha-2-to-flag "ML")          ; Mali
  "MLT" (alpha-2-to-flag "MT")          ; Malta
  "MMR" (alpha-2-to-flag "MM")          ; Myanmar
  "MNE" (alpha-2-to-flag "ME")          ; Montenegro
  "MNG" (alpha-2-to-flag "MN")          ; Mongolia
  "MNP" (alpha-2-to-flag "MP")          ; Northern Mariana Islands (the)
  "MOZ" (alpha-2-to-flag "MZ")          ; Mozambique
  "MRT" (alpha-2-to-flag "MR")          ; Mauritania
  "MSR" (alpha-2-to-flag "MS")          ; Montserrat
  "MTQ" (alpha-2-to-flag "MQ")          ; Martinique
  "MUS" (alpha-2-to-flag "MU")          ; Mauritius
  "MWI" (alpha-2-to-flag "MW")          ; Malawi
  "MYS" (alpha-2-to-flag "MY")          ; Malaysia
  "MYT" (alpha-2-to-flag "YT")          ; Mayotte
  "NAM" (alpha-2-to-flag "NA")          ; Namibia
  "NCL" (alpha-2-to-flag "NC")          ; New Caledonia
  "NER" (alpha-2-to-flag "NE")          ; Niger (the)
  "NFK" (alpha-2-to-flag "NF")          ; Norfolk Island
  "NGA" (alpha-2-to-flag "NG")          ; Nigeria
  "NIC" (alpha-2-to-flag "NI")          ; Nicaragua
  "NIU" (alpha-2-to-flag "NU")          ; Niue
  "NLD" (alpha-2-to-flag "NL")          ; Netherlands (the)
  "NOR" (alpha-2-to-flag "NO")          ; Norway
  "NPL" (alpha-2-to-flag "NP")          ; Nepal
  "NRU" (alpha-2-to-flag "NR")          ; Nauru
  "NZL" (alpha-2-to-flag "NZ")          ; New Zealand
  "OMN" (alpha-2-to-flag "OM")          ; Oman
  "PAK" (alpha-2-to-flag "PK")          ; Pakistan
  "PAN" (alpha-2-to-flag "PA")          ; Panama
  "PCN" (alpha-2-to-flag "PN")          ; Pitcairn
  "PER" (alpha-2-to-flag "PE")          ; Peru
  "PHL" (alpha-2-to-flag "PH")          ; Philippines (the)
  "PLW" (alpha-2-to-flag "PW")          ; Palau
  "PNG" (alpha-2-to-flag "PG")          ; Papua New Guinea
  "POL" (alpha-2-to-flag "PL")          ; Poland
  "PRI" (alpha-2-to-flag "PR")          ; Puerto Rico
  "PRK" (alpha-2-to-flag "KP")          ; Korea (the Democratic People's Republic of)
  "PRT" (alpha-2-to-flag "PT")          ; Portugal
  "PRY" (alpha-2-to-flag "PY")          ; Paraguay
  "PSE" (alpha-2-to-flag "PS")          ; Palestine, State of
  "PYF" (alpha-2-to-flag "PF")          ; French Polynesia
  "QAT" (alpha-2-to-flag "QA")          ; Qatar
  "REU" (alpha-2-to-flag "RE")          ; Réunion
  "ROU" (alpha-2-to-flag "RO")          ; Romania
  "RUS" (alpha-2-to-flag "RU")          ; Russian Federation (the)
  "RWA" (alpha-2-to-flag "RW")          ; Rwanda
  "SAU" (alpha-2-to-flag "SA")          ; Saudi Arabia
  "SDN" (alpha-2-to-flag "SD")          ; Sudan (the)
  "SEN" (alpha-2-to-flag "SN")          ; Senegal
  "SGP" (alpha-2-to-flag "SG")          ; Singapore
  "SGS" (alpha-2-to-flag "GS")          ; South Georgia and the South Sandwich Islands
  "SHN" (alpha-2-to-flag "SH")          ; Saint Helena, Ascension and Tristan da Cunha
  "SJM" (alpha-2-to-flag "SJ")          ; Svalbard and Jan Mayen
  "SLB" (alpha-2-to-flag "SB")          ; Solomon Islands
  "SLE" (alpha-2-to-flag "SL")          ; Sierra Leone
  "SLV" (alpha-2-to-flag "SV")          ; El Salvador
  "SMR" (alpha-2-to-flag "SM")          ; San Marino
  "SOM" (alpha-2-to-flag "SO")          ; Somalia
  "SPM" (alpha-2-to-flag "PM")          ; Saint Pierre and Miquelon
  "SRB" (alpha-2-to-flag "RS")          ; Serbia
  "SSD" (alpha-2-to-flag "SS")          ; South Sudan
  "STP" (alpha-2-to-flag "ST")          ; Sao Tome and Principe
  "SUR" (alpha-2-to-flag "SR")          ; Suriname
  "SVK" (alpha-2-to-flag "SK")          ; Slovakia
  "SVN" (alpha-2-to-flag "SI")          ; Slovenia
  "SWE" (alpha-2-to-flag "SE")          ; Sweden
  "SWZ" (alpha-2-to-flag "SZ")          ; Eswatini
  "SXM" (alpha-2-to-flag "SX")          ; Sint Maarten (Dutch part)
  "SYC" (alpha-2-to-flag "SC")          ; Seychelles
  "SYR" (alpha-2-to-flag "SY")          ; Syrian Arab Republic
  "TCA" (alpha-2-to-flag "TC")          ; Turks and Caicos Islands (the)
  "TCD" (alpha-2-to-flag "TD")          ; Chad
  "TGO" (alpha-2-to-flag "TG")          ; Togo
  "THA" (alpha-2-to-flag "TH")          ; Thailand
  "TJK" (alpha-2-to-flag "TJ")          ; Tajikistan
  "TKL" (alpha-2-to-flag "TK")          ; Tokelau
  "TKM" (alpha-2-to-flag "TM")          ; Turkmenistan
  "TLS" (alpha-2-to-flag "TL")          ; Timor-Leste
  "TON" (alpha-2-to-flag "TO")          ; Tonga
  "TTO" (alpha-2-to-flag "TT")          ; Trinidad and Tobago
  "TUN" (alpha-2-to-flag "TN")          ; Tunisia
  "TUR" (alpha-2-to-flag "TR")          ; Turkey
  "TUV" (alpha-2-to-flag "TV")          ; Tuvalu
  "TWN" (alpha-2-to-flag "TW")          ; Taiwan (Province of China)
  "TZA" (alpha-2-to-flag "TZ")          ; Tanzania, United Republic of
  "UGA" (alpha-2-to-flag "UG")          ; Uganda
  "UKR" (alpha-2-to-flag "UA")          ; Ukraine
  "UMI" (alpha-2-to-flag "UM")          ; United States Minor Outlying Islands (the)
  "URY" (alpha-2-to-flag "UY")          ; Uruguay
  "USA" (alpha-2-to-flag "US")          ; United States of America (the)
  "UZB" (alpha-2-to-flag "UZ")          ; Uzbekistan
  "VAT" (alpha-2-to-flag "VA")          ; Holy See (the)
  "VCT" (alpha-2-to-flag "VC")          ; Saint Vincent and the Grenadines
  "VEN" (alpha-2-to-flag "VE")          ; Venezuela (Bolivarian Republic of)
  "VGB" (alpha-2-to-flag "VG")          ; Virgin Islands (British)
  "VIR" (alpha-2-to-flag "VI")          ; Virgin Islands (U.S.)
  "VNM" (alpha-2-to-flag "VN")          ; Viet Nam
  "VUT" (alpha-2-to-flag "VU")          ; Vanuatu
  "WLF" (alpha-2-to-flag "WF")          ; Wallis and Futuna
  "WSM" (alpha-2-to-flag "WS")          ; Samoa
  "YEM" (alpha-2-to-flag "YE")          ; Yemen
  "ZAF" (alpha-2-to-flag "ZA")          ; South Africa
  "ZMB" (alpha-2-to-flag "ZM")          ; Zambia
  "ZWE" (alpha-2-to-flag "ZW")          ; Zimbabwe

  ; 'bonus' 3 letter codes, including some returned by football-data.org
  "EUR" (alpha-2-to-flag "EU")          ; Europe
  "INT" "🗺️"                       ; World - could also use (alpha-2-to-flag "UN")
  "AFR" "🌍"                       ; Africa
  "RSA" (alpha-2-to-flag "ZA")     ; South Africa (alternative code)
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

(defn emoji
  "Returns the emoji flag (a string) for the given country-code (an ISO-3166-1 alpha-2 or alpha-3 code, or one of the 'bonus' 3 letter codes used by football-data.org).  Returns nil for invalid 3-letter codes, or another, non-flag Unicode character for an invalid 2-letter code."
  [country-code]
  (when country-code
    (let [code (s/upper-case (s/trim country-code))]
      (case (count code)
        2 (alpha-2-to-flag code)
        3 (get alpha-3-to-flag code)
        nil))))
