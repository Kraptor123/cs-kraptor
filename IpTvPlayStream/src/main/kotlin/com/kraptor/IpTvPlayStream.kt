// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup

class IpTvPlayStream : MainAPI() {
    override var mainUrl              = "https://iptvplay.stream"
    override var name                 = "IpTvPlay"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.Live)

    override val mainPage = mainPageOf(
        "${mainUrl}/country/turkey"                            to "Turkey",
        "${mainUrl}/country/united-kingdom"                    to "United Kingdom",
        "${mainUrl}/country/united-states"                     to "United States",
        "${mainUrl}/country/canada"                            to "Canada",
        "${mainUrl}/country/germany"                           to "Germany",
        "${mainUrl}/country/bulgaria"                          to "Bulgaria",
        "${mainUrl}/country/cyprus"                            to "Cyprus",
        "${mainUrl}/country/romania"                           to "Romania",
        "${mainUrl}/country/russia"                            to "Russia",
        "${mainUrl}/country/albania"                           to "Albania",
        "${mainUrl}/country/czech-republic"                    to "Czech Republic",
        "${mainUrl}/country/france"                            to "France",
        "${mainUrl}/country/italy"                             to "Italy",
        "${mainUrl}/country/japan"                             to "Japan",
        "${mainUrl}/country/azerbaijan"                        to "Azerbaijan",
        "${mainUrl}/country/kazakhstan"                        to "Kazakhstan",
        "${mainUrl}/country/kyrgyzstan"                        to "Kyrgyzstan",
        "${mainUrl}/country/turkmenistan"                      to "Turkmenistan",
        "${mainUrl}/country/uzbekistan"                        to "Uzbekistan",
        "${mainUrl}/country/afghanistan"                       to "Afghanistan",
        "${mainUrl}/country/algeria"                           to "Algeria",
//        "${mainUrl}/country/american-samoa"                    to "American Samoa",
//        "${mainUrl}/country/andorra"                           to "Andorra",
//        "${mainUrl}/country/angola"                            to "Angola",
//        "${mainUrl}/country/anguilla"                          to "Anguilla",
//        "${mainUrl}/country/antigua-and-barbuda"               to "Antigua and Barbuda",
        "${mainUrl}/country/argentina"                         to "Argentina",
//        "${mainUrl}/country/armenia"                           to "Armenia",
//        "${mainUrl}/country/aruba"                             to "Aruba",
        "${mainUrl}/country/australia"                         to "Australia",
        "${mainUrl}/country/austria"                           to "Austria",
//        "${mainUrl}/country/bahamas"                           to "Bahamas",
//        "${mainUrl}/country/bahrain"                           to "Bahrain",
//        "${mainUrl}/country/bangladesh"                        to "Bangladesh",
//        "${mainUrl}/country/barbados"                          to "Barbados",
        "${mainUrl}/country/belarus"                           to "Belarus",
        "${mainUrl}/country/belgium"                           to "Belgium",
//        "${mainUrl}/country/belize"                            to "Belize",
//        "${mainUrl}/country/benin"                             to "Benin",
//        "${mainUrl}/country/bermuda"                           to "Bermuda",
//        "${mainUrl}/country/bhutan"                            to "Bhutan",
        "${mainUrl}/country/bolivia"                           to "Bolivia",
//        "${mainUrl}/country/bonaire"                           to "Bonaire",
        "${mainUrl}/country/bosnia-and-herzegovina"            to "Bosnia and Herzegovina",
//        "${mainUrl}/country/botswana"                          to "Botswana",
//        "${mainUrl}/country/bouvet-island"                     to "Bouvet Island",
        "${mainUrl}/country/brazil"                            to "Brazil",
//        "${mainUrl}/country/british-virgin-islands"            to "British Virgin Islands",
//        "${mainUrl}/country/brunei"                            to "Brunei",
//        "${mainUrl}/country/burkina-faso"                      to "Burkina Faso",
        "${mainUrl}/country/burundi"                           to "Burundi",
        "${mainUrl}/country/cambodia"                          to "Cambodia",
        "${mainUrl}/country/cameroon"                          to "Cameroon",
//        "${mainUrl}/country/cape-verde"                        to "Cape Verde",
//        "${mainUrl}/country/cayman-islands"                    to "Cayman Islands",
//        "${mainUrl}/country/central-african-republic"          to "Central African Republic",
//        "${mainUrl}/country/chad"                              to "Chad",
//        "${mainUrl}/country/chile"                             to "Chile",
        "${mainUrl}/country/china"                             to "China",
        "${mainUrl}/country/colombia"                          to "Colombia",
//        "${mainUrl}/country/comoros"                           to "Comoros",
//        "${mainUrl}/country/cook-islands"                      to "Cook Islands",
        "${mainUrl}/country/costa-rica"                        to "Costa Rica",
        "${mainUrl}/country/croatia"                           to "Croatia",
        "${mainUrl}/country/cuba"                              to "Cuba",
//        "${mainUrl}/country/curacao"                           to "Curacao",
//        "${mainUrl}/country/democratic-republic-of-the-congo"  to "Democratic Republic of the Congo",
        "${mainUrl}/country/denmark"                           to "Denmark",
//        "${mainUrl}/country/djibouti"                          to "Djibouti",
//        "${mainUrl}/country/dominica"                          to "Dominica",
        "${mainUrl}/country/dominican-republic"                to "Dominican Republic",
//        "${mainUrl}/country/east-timor"                        to "East Timor",
        "${mainUrl}/country/ecuador"                           to "Ecuador",
        "${mainUrl}/country/egypt"                             to "Egypt",
        "${mainUrl}/country/el-salvador"                       to "El Salvador",
//        "${mainUrl}/country/equatorial-guinea"                 to "Equatorial Guinea",
//        "${mainUrl}/country/eritrea"                           to "Eritrea",
        "${mainUrl}/country/estonia"                           to "Estonia",
        "${mainUrl}/country/ethiopia"                          to "Ethiopia",
//        "${mainUrl}/country/falkland-islands"                  to "Falkland Islands",
//        "${mainUrl}/country/faroe-islands"                     to "Faroe Islands",
        "${mainUrl}/country/fiji"                              to "Fiji",
        "${mainUrl}/country/finland"                           to "Finland",
//        "${mainUrl}/country/french-guiana"                     to "French Guiana",
//        "${mainUrl}/country/french-polynesia"                  to "French Polynesia",
//        "${mainUrl}/country/french-southern-territories"       to "French Southern Territories",
//        "${mainUrl}/country/gabon"                             to "Gabon",
        "${mainUrl}/country/gambia"                            to "Gambia",
        "${mainUrl}/country/georgia"                           to "Georgia",
        "${mainUrl}/country/ghana"                             to "Ghana",
        "${mainUrl}/country/greece"                            to "Greece",
//        "${mainUrl}/country/greenland"                         to "Greenland",
//        "${mainUrl}/country/grenada"                           to "Grenada",
//        "${mainUrl}/country/guadeloupe"                        to "Guadeloupe",
//        "${mainUrl}/country/guam"                              to "Guam",
        "${mainUrl}/country/guatemala"                         to "Guatemala",
        "${mainUrl}/country/guinea"                            to "Guinea",
//        "${mainUrl}/country/guinea-bissau"                     to "Guinea-Bissau",
//        "${mainUrl}/country/guyana"                            to "Guyana",
//        "${mainUrl}/country/haiti"                             to "Haiti",
//        "${mainUrl}/country/honduras"                          to "Honduras",
        "${mainUrl}/country/hong-kong"                         to "Hong Kong",
        "${mainUrl}/country/hungary"                           to "Hungary",
        "${mainUrl}/country/iceland"                           to "Iceland",
        "${mainUrl}/country/india"                             to "India",
        "${mainUrl}/country/indonesia"                         to "Indonesia",
        "${mainUrl}/country/iran"                              to "Iran",
        "${mainUrl}/country/iraq"                              to "Iraq",
        "${mainUrl}/country/ireland"                           to "Ireland",
        "${mainUrl}/country/israel"                            to "Israel",
//        "${mainUrl}/country/ivory-coast"                       to "Ivory Coast",
        "${mainUrl}/country/jamaica"                           to "Jamaica",
        "${mainUrl}/country/jordan"                            to "Jordan",
        "${mainUrl}/country/kenya"                             to "Kenya",
//        "${mainUrl}/country/kiribati"                          to "Kiribati",
        "${mainUrl}/country/kosovo"                            to "Kosovo",
        "${mainUrl}/country/kuwait"                            to "Kuwait",
//        "${mainUrl}/country/laos"                              to "Laos",
//        "${mainUrl}/country/latvia"                            to "Latvia",
        "${mainUrl}/country/lebanon"                           to "Lebanon",
//        "${mainUrl}/country/lesotho"                           to "Lesotho",
        "${mainUrl}/country/liberia"                           to "Liberia",
        "${mainUrl}/country/libya"                             to "Libya",
        "${mainUrl}/country/liechtenstein"                     to "Liechtenstein",
        "${mainUrl}/country/lithuania"                         to "Lithuania",
        "${mainUrl}/country/luxembourg"                        to "Luxembourg",
//        "${mainUrl}/country/macao"                             to "Macao",
//        "${mainUrl}/country/madagascar"                        to "Madagascar",
//        "${mainUrl}/country/malawi"                            to "Malawi",
        "${mainUrl}/country/malaysia"                          to "Malaysia",
//        "${mainUrl}/country/maldives"                          to "Maldives",
//        "${mainUrl}/country/mali"                              to "Mali",
//        "${mainUrl}/country/malta"                             to "Malta",
//        "${mainUrl}/country/marshall-islands"                  to "Marshall Islands",
//        "${mainUrl}/country/martinique"                        to "Martinique",
//        "${mainUrl}/country/mauritania"                        to "Mauritania",
//        "${mainUrl}/country/mauritius"                         to "Mauritius",
//        "${mainUrl}/country/mayotte"                           to "Mayotte",
        "${mainUrl}/country/mexico"                            to "Mexico",
//        "${mainUrl}/country/micronesia"                        to "Micronesia",
//        "${mainUrl}/country/moldova"                           to "Moldova",
//        "${mainUrl}/country/monaco"                            to "Monaco",
        "${mainUrl}/country/mongolia"                          to "Mongolia",
//        "${mainUrl}/country/montenegro"                        to "Montenegro",
//        "${mainUrl}/country/montserrat"                        to "Montserrat",
        "${mainUrl}/country/morocco"                           to "Morocco",
        "${mainUrl}/country/mozambique"                        to "Mozambique",
//        "${mainUrl}/country/namibia"                           to "Namibia",
//        "${mainUrl}/country/nauru"                             to "Nauru",
        "${mainUrl}/country/nepal"                             to "Nepal",
//        "${mainUrl}/country/netherlands"                       to "Netherlands",
//        "${mainUrl}/country/new-caledonia"                     to "New Caledonia",
//        "${mainUrl}/country/new-zealand"                       to "New Zealand",
//        "${mainUrl}/country/nicaragua"                         to "Nicaragua",
//        "${mainUrl}/country/niger"                             to "Niger",
//        "${mainUrl}/country/nigeria"                           to "Nigeria",
//        "${mainUrl}/country/niue"                              to "Niue",
//        "${mainUrl}/country/norfolk-island"                    to "Norfolk Island",
//        "${mainUrl}/country/north-korea"                       to "North Korea",
//        "${mainUrl}/country/north-macedonia"                   to "North Macedonia",
//        "${mainUrl}/country/northern-mariana-islands"          to "Northern Mariana Islands",
//        "${mainUrl}/country/norway"                            to "Norway",
//        "${mainUrl}/country/oman"                              to "Oman",
//        "${mainUrl}/country/pakistan"                          to "Pakistan",
//        "${mainUrl}/country/palau"                             to "Palau",
//        "${mainUrl}/country/palestine"                         to "Palestine",
        "${mainUrl}/country/panama"                            to "Panama",
//        "${mainUrl}/country/papua-new-guinea"                  to "Papua New Guinea",
//        "${mainUrl}/country/paraguay"                          to "Paraguay",
//        "${mainUrl}/country/peru"                              to "Peru",
        "${mainUrl}/country/philippines"                       to "Philippines",
//        "${mainUrl}/country/pitcairn-islands"                  to "Pitcairn Islands",
        "${mainUrl}/country/poland"                            to "Poland",
//        "${mainUrl}/country/portugal"                          to "Portugal",
//        "${mainUrl}/country/puerto-rico"                       to "Puerto Rico",
//        "${mainUrl}/country/qatar"                             to "Qatar",
//        "${mainUrl}/country/republic-of-the-congo"             to "Republic of the Congo",
//        "${mainUrl}/country/reunion"                           to "Réunion",
//        "${mainUrl}/country/rwanda"                            to "Rwanda",
//        "${mainUrl}/country/saint-barthelemy"                  to "Saint Barthélemy",
//        "${mainUrl}/country/saint-helena"                      to "Saint Helena",
//        "${mainUrl}/country/saint-kitts-and-nevis"             to "Saint Kitts and Nevis",
//        "${mainUrl}/country/saint-lucia"                       to "Saint Lucia",
//        "${mainUrl}/country/saint-martin"                      to "Saint Martin",
//        "${mainUrl}/country/saint-pierre-and-miquelon"         to "Saint Pierre and Miquelon",
//        "${mainUrl}/country/saint-vincent-and-the-grenadines"  to "Saint Vincent and the Grenadines",
//        "${mainUrl}/country/samoa"                             to "Samoa",
//        "${mainUrl}/country/san-marino"                        to "San Marino",
//        "${mainUrl}/country/sao-tome-and-principe"             to "São Tomé and Príncipe",
        "${mainUrl}/country/saudi-arabia"                      to "Saudi Arabia",
//        "${mainUrl}/country/senegal"                           to "Senegal",
        "${mainUrl}/country/serbia"                            to "Serbia",
//        "${mainUrl}/country/seychelles"                        to "Seychelles",
//        "${mainUrl}/country/sierra-leone"                      to "Sierra Leone",
        "${mainUrl}/country/singapore"                         to "Singapore",
//        "${mainUrl}/country/sint-maarten"                      to "Sint Maarten",
        "${mainUrl}/country/slovakia"                          to "Slovakia",
//        "${mainUrl}/country/slovenia"                          to "Slovenia",
//        "${mainUrl}/country/solomon-islands"                   to "Solomon Islands",
//        "${mainUrl}/country/somalia"                           to "Somalia",
        "${mainUrl}/country/south-africa"                      to "South Africa",
//        "${mainUrl}/country/south-georgia-and-the-south-sandwich-islands"     to "South Georgia and the South Sandwich Islands",
        "${mainUrl}/country/south-korea"                       to "South Korea",
//        "${mainUrl}/country/south-sudan"                       to "South Sudan",
        "${mainUrl}/country/spain"                             to "Spain",
        "${mainUrl}/country/sri-lanka"                         to "Sri Lanka",
        "${mainUrl}/country/sudan"                             to "Sudan",
//        "${mainUrl}/country/suriname"                          to "Suriname",
//        "${mainUrl}/country/swaziland"                         to "Swaziland",
        "${mainUrl}/country/sweden"                            to "Sweden",
        "${mainUrl}/country/switzerland"                       to "Switzerland",
//        "${mainUrl}/country/syria"                             to "Syria",
//        "${mainUrl}/country/taiwan"                            to "Taiwan",
        "${mainUrl}/country/tajikistan"                        to "Tajikistan",
//        "${mainUrl}/country/tanzania"                          to "Tanzania",
        "${mainUrl}/country/thailand"                          to "Thailand",
//        "${mainUrl}/country/togo"                              to "Togo",
//        "${mainUrl}/country/tokelau"                           to "Tokelau",
//        "${mainUrl}/country/tonga"                             to "Tonga",
//        "${mainUrl}/country/trinidad-and-tobago"               to "Trinidad and Tobago",
//        "${mainUrl}/country/tunisia"                           to "Tunisia",
        "${mainUrl}/country/turks-and-caicos-islands"          to "Turks and Caicos Islands",
//        "${mainUrl}/country/tuvalu"                            to "Tuvalu",
//        "${mainUrl}/country/u-s-virgin-islands"                to "U.S. Virgin Islands",
        "${mainUrl}/country/uganda"                            to "Uganda",
        "${mainUrl}/country/ukraine"                           to "Ukraine",
        "${mainUrl}/country/united-arab-emirates"              to "United Arab Emirates",
        "${mainUrl}/country/uruguay"                           to "Uruguay",
//        "${mainUrl}/country/vanuatu"                           to "Vanuatu",
        "${mainUrl}/country/vatican-city"                      to "Vatican City",
        "${mainUrl}/country/venezuela"                         to "Venezuela",
        "${mainUrl}/country/vietnam"                           to "Vietnam",
//        "${mainUrl}/country/wallis-and-futuna"                 to "Wallis and Futuna",
//        "${mainUrl}/country/western-sahara"                    to "Western Sahara",
//        "${mainUrl}/country/yemen"                             to "Yemen",
//        "${mainUrl}/country/zambia"                            to "Zambia",
        "${mainUrl}/country/zimbabwe"                          to "Zimbabwe",
        "${mainUrl}/country/international"                     to "International"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // request.data içinde zaten tam URL var
        val document = Jsoup.connect(request.data)
            .ignoreContentType(true) // JSON çekmek için değil ama ek bir güvence
            .get()

        // Tüm <a> etiketlerini dolaşıp, poster ve başlık varsa map’le
        val home = document.select("a")
            .mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = selectFirst("h2")?.text() ?: return null
        val href      = fixUrlNull(selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(selectFirst("img")?.attr("src"))

        return newLiveSearchResponse(
            name = title,
            url  = href,
            type = TvType.Live
        ) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst(".w-24 img")?.attr("src"))
        val description     = document.selectFirst("p.text-base")?.text()?.trim()

        return newLiveStreamLoadResponse(title, url = url, url) {
            this.posterUrl       = poster
            this.plot            = description
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("iptvpl", "data = ${data}")
        val document = app.get(data).document

        val regex = Regex("\"m3u8Url\\\\\":\\\\\"([^\"]+)\\\\\"", options = setOf(RegexOption.IGNORE_CASE)).find(document.html())


        val icerik = regex?.groupValues?.get(1).toString()


        val icerikIcerigi = fixUrlNull(icerik).toString()

        Log.d("iptvpl", "icerikicerigi = ${icerikIcerigi}")

        callback.invoke(
            newExtractorLink(
                "IpTvPlay",
                "IpTvPlay",
                icerikIcerigi,
                ExtractorLinkType.M3U8
            ) {
                this.referer = "${mainUrl}/"
                this.headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:138.0) Gecko/20100101 Firefox/138.0",
                    "Connection" to "keep-alive",
                    "Referer" to mainUrl,
                )
            })
        return true
    }
}