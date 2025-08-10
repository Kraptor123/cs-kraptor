// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class WebDramaTurkey : MainAPI() {
    override var mainUrl              = "https://webdramaturkey.org"
    override var name                 = "WebDramaTurkey"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.AsianDrama)
    override val mainPage = mainPageOf(
        "${mainUrl}/" to "Son Bölümler",
        "${mainUrl}/diziler"             to "Diziler",
        "${mainUrl}/filmler"             to "Filmler",
        "${mainUrl}/animeler"            to "Animeler",
        "${mainUrl}/tur/dram"            to "Dram",
        "${mainUrl}/tur/fantastik"       to "Fantastik",
        "${mainUrl}/tur/gerilim"         to "Gerilim",
        "${mainUrl}/tur/komedi"          to "Komedi",
        "${mainUrl}/tur/korku"           to "Korku",
        "${mainUrl}/tur/lise"            to "Lise",
        "${mainUrl}/tur/ofis-aski"       to "Ofis Aşkı",
        "${mainUrl}/tur/romantik"        to "Romantik",
        "${mainUrl}/tur/universite"      to "Üniversite",
        "${mainUrl}/tur/aile"            to "Aile",
        "${mainUrl}/tur/aksiyon"         to "Aksiyon",
        "${mainUrl}/tur/belgesel"        to "Belgesel",
        "${mainUrl}/tur/bilim-kurgu"     to "Bilim Kurgu",
       
        "${mainUrl}/tur/eglence"         to "Eğlence",
        "${mainUrl}/tur/genclik"         to "Gençlik",
       
        "${mainUrl}/tur/gizem"           to "Gizem",
       
        
        "${mainUrl}/tur/reality"         to "Reality",
        "${mainUrl}/tur/tarihi"          to "Tarihi",
        
        "${mainUrl}/tur/varyete"         to "Varyete",
        "${mainUrl}/tur/web-drama"       to "Web Drama",
        "${mainUrl}/tur/wuxia"           to "Wuxia",
        "${mainUrl}/tur/xianxia"         to "Xianxia",
        "${mainUrl}/tur/yarisma"         to "Yarışma",
        
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}${if (request.data == mainUrl + "/") "" else "?page=$page"}").document
        
        val home = if (request.data == mainUrl + "/") {
           
            document.select("div.col.sonyuklemeler").mapNotNull { it.toLatestEpisodeResult() }
        } else {
           
            document.select("div.col").mapNotNull { it.toMainPageResult() }
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val boyslove = listOf(
            "Desire",
            "40 Made ni Shitai 10 no Koto",
            "Ameagari no Bokura ni Tsuite",
            "A Sketchy Job",
            "Missing You",
            "The Promise of the Soul",
            "Wrong Number",
            "Sotus",
            "7 Times 4 Days",
            "Shine",
            "Revenged Love",
            "Boys Vibe The Project",
            "Suntiny",
            "Hishakai Shindo",
            "I Became the Lead in a BL Drama",
            "Love Upon a Time",
            "Goddess Bless You from Death",
            "Dare You to Death",
            "Cat for Cash",
            "Memoir of Rati",
            "Ball Boy Tactics",
            "You(r)Tuber",
            "Interminable",
            "Love You Teacher",
            "Trapped in Osaka",
            "My Sweet Brother in Law",
            "The Sparkle In Your Eye",
            "Moon and Dust",
            "Infidelity",
            "It's a Disease",
            "I Promise I Will Come Back",
            "Loy Kaew First Love",
            "I'm the Most Beautiful Count",
            "Vice Versa",
            "Lovely Writer",
            "Season of Love in Shimane",
            "A Dog and a Plane",
            "My Magic Prophecy",
            "Reset",
            "Theory of Love",
            "The Ex-Morning",
            "Eye Contact",
            "Me and Thee",
            "Burnout Syndrome",
            "Ticket to Heaven",
            "Khemjira",
            "Sweetheart Service",
            "Mission to the Moon",
            "Never Fair",
            "Secrets Happened on the Litchi Island",
            "The Bangkok Boy",
            "Knock Out",
            "My Sweetheart Jom",
            "Love Casting",
            "La Pluie",
            "My Stubborn",
            "Secret Ghost",
            "My Tooth Your Love",
            "Chains of Heart",
            "Something Is Not Right",
            "Boys in Love",
            "The Racing Track",
            "Business as Usual",
            "Heesu in Class 2",
            "Fight for You",
            "The Tasty Florida",
            "Lost in the Woods",
            "Sweet Tooth",
            "Good Dentist",
            "Close To You",
            "Fish Upon the Sky",
            "Last Twilight in Phuket",
            "Dark Blue Kiss",
            "The Miracle of Teddy Bear",
            "The Eclipse",
            "Why R U?",
            "Last Meal Universe",
            "Secret Relationships",
            "My Golden Blood",
            "A Perfect Match",
            "PorschArm the Wedding",
            "The Last Time",
            "Backlight",
            "Sweet Curse",
            "Fight for Love",
            "Heart Stain",
            "Great Men Academy",
            "The Renovation",
            "Checkered Shirt",
            "Top Form",
            "Gelboys",
            "Never Let Me Go",
            "Flirt Milk",
            "FC Soldout",
            "Silent Sparks",
            "Exclusive Love",
            "HIStory4: Close to You",
            "My School President",
            "Impression of Youth",
            "The Paradise of Thorns",
            "I'll Turn Back This Time",
            "Bad to Bed",
            "When It Rains, It Pours",
            "Laws of Attraction",
            "The Boy Next World",
            "The Shorthest Distance is Round - Rain and Soda +21",
            "The Shortest Distance is Round - Blanc +21",
            "Only for Fans",
            "Sangmin Dinneaw",
            "The Shortest Distance - Fallen Flowers +18",
            "Summerdaze",
            "Manner of Death",
            "The Novelist - Continued Spring Life",
            "Eternal Butler",
            "That's My Candy",
            "Bridge of Destiny",
            "Goodbye Mother",
            "The Novelist",
            "Bad Buddy",
            "Ossan's Love",
            "Innocent",
            "Capture Lover",
            "Craving You",
            "Ossan's Love",
            "Love in the Big City",
            "Oh! My Assistant",
            "HIStory1",
            "I Feel You Linger in the Air",
            "Mood Indigo",
            "0.5D",
            "Spare Me Your Mercy",
            "ThamePo Heart That Skips a Beat",
            "Winter Is Not the Death of Summer but the Birth of Spring",
            "Let Me Hear It Barefoot",
            "Triage",
            "Stay with Me",
            "Method",
            "Love by Chance",
            "The Nipple Talk",
            "A Balloon's Landing",
            "Secret Love (TH)",
            "Caged Again",
            "Our Youth",
            "The Middleman's Love",
            "Stay by My Side",
            "The Heart Killers",
            "More than Words",
            "Love in the Air: Koi no Yokan",
            "Perfect 10 Liners",
            "Make Up",
            "Make Me Grow Up!",
            "Love in Translation",
            "Dear Doctor, I'm Coming for Soul",
            "HIStory3: Trapped",
            "See Your Love",
            "I Promised You the Moon",
            "I Told Sunset about You",
            "The Shortest Distance Is Round: Noir / +21",
            "The End of the World",
            "Let Free the Curse of Taekwondo",
            "Polyethylene Terephthalate",
            "Heavens x Candy +21",
            "Love in the Big City",
            "Eccentric Romance",
            "Every You, Every Me",
            "Love Mechanics",
            "My Damn Business",
            "Uncle Unknown",
            "Fourever You",
            "Teenager Judge",
            "Ghost Host Ghost House",
            "Still 2gether",
            "The Cornered Mouse Dreams of Cheese",
            "Original Sin",
            "Moonlight Chicken",
            "2gether",
            "KinnPorsche",
            "A Tale of Thousand Stars",
            "Domestic Incident",
            "HIStory2: Crossing the Line",
            "Smells Like Green Spirit",
            "Doku Koi: Doku mo Sugireba Koi to Naru",
            "Bad Guy My Boss",
            "Eternal Yesterday",
            "Love Sick",
            "The Time of Fever",
            "Jack & Joker: U Steal My Heart!",
            "PD",
            "Plus & Minus",
            "The Sign",
            "Love Bill",
            "I Cannot Reach You",
            "Happy of the End",
            "The Hidden Moon",
            "Live in Love",
            "We Best Love",
            "Because of You",
            "Jack o' Frost",
            "Between Us",
            "Kidnap",
            "Memory in the Letter",
            "Until We Meet Again",
            "The On1y One",
            "Seoul Blues",
            "Ossan’s Love: LOVE or DEAD",
            "Only Boo!",
            "Monster Next Door",
            "First Note of Love",
            "Addicted Heroin",
            "Silhouette of Your Voice",
            "Blue Canvas of Youthful Days",
            "Oxygen",
            "Battle of the Writers",
            "Be Loved in House: I Do",
            "Don't Say No",
            "Cosmetic Playlover",
            "Sugar Dog Life",
            "My Bromance",
            "Bad Guy",
            "HI Story 2: Right or Wrong",
            "Remember Me",
            "Love in the Air",
            "Kiseki: Dear to Me",
            "Candy Color Paradox",
            "Oh! Boarding House",
            "Not Me",
            "Once Again",
            "Kamisama no Ekohiiki",
            "4 Minutes",
            "I Saw You in My Dream",
            "Where Your Eyes Linger",
            "My Beautiful Man: Eternal",
            "Utsukushii Kare",
            "One Room Angel",
            "The Boyfriend",
            "8.2 Byo no Hosoku",
            "Hold",
            "Mr. Unlucky Has No Choice but to Kiss!",
            "Takara-kun to Amagi-kun",
            "A Distant Place",
            "Meet You at the Blossom",
            "Century of Love",
            "This Love Doesn't Have Long Beans",
            "Takara no Vidro",
            "Hidamari ga Kikoeru",
            "The Trainee",
            "His Man 3",
            "Spaceless",
            "The Rebound",
            "SunsetxVibes",
            "Under the Oak Tree",
            "Your Sky",
            "Cityboy_Log",
            "Egoist",
            "Inverse Identity",
            "The Eighth Sense",
            "30-sai made Dotei Da to Mahotsukai ni Nareru rashii",
            "Wandee Goodday",
            "Blue Boys",
            "A Man Who Defies the World of BL",
            "My Stand In",
            "At 25:00 in Akasaka",
            "Living with Him",
            "The Next Prince",
            "We Are",
            "Gray Shelter",
            "Boys Be Brave",
            "TharnType",
            "Two Worlds",
            "City of Stars",
            "Kiseki Chapter 2",
            "Deep Night",
            "Love Is like a Cat",
            "Jazz for Two",
            "Love isBetter the Second Time Around",
            "Bed Friend",
            "Taikan Yoho",
            "Old Fashion Cupcake",
            "To Be Continued",
            "Kieta Hatsukoi",
            "Cutie Pie",
            "Mr. Heart",
            "A Secretly Love",
            "My Strawberry Film",
            "Love Sea",
            "My Sweet Dear",
            "1000 Years Old",
            "Unknown",
            "Happy Ending",
            "Although I Love You and You?",
            "After Sundown",
            "Perfect Propose",
            "Anti Reset",
            "About Youth",
            "My Love Mix-Up!",
            "Love for Love's Sake",
            "Last Twilight",
            "Dead Friend Forever - DFF",
            "Night Dream",
            "Playboyy",
            "Behind the Shadows",
            "Your Name Engraved Herein",
            "A Breeze of Love",
            "Pit Babe",
            "Shadow",
            "Wuju Bakery",
            "Cherry Magic",
            "My Dear Gangster Oppa",
            "Bump Up Business",
            "If It's With You",
            "Absolute Zero",
            "Dear Ex",
            "Crazy Handsome Rich",
            "My Universe",
            "Only Friends",
            "Red, White & Royal Blue",
            "Marry My Dead Body",
            "Sing My Crush",
            "Jun & Jun",
            "Love Tractor",
            "Star Struck",
            "Love Mate",
            "The Director Who Buys Me Dinner",
            "Happy Merry Ending",
            "All the Liquors",
            "Our Dating Sim",
            "Unintentional Love Story",
            "A Shoulder to Cry On",
            "Choco Milk Shake",
            "His Man",
            "Merry Queer",
            "Love Class",
            "Ocean Likes Me",
            "Blueming",
            "Private Lessons",
            "Blue of Winter",
            "Cherry Blossoms After Winter",
            "Behind Cut",
            "Color Rush 2",
            "Color Rush",
            "First Love Again",
            "Oh! Boarding House",
            "To My Star",
            "Semantic Error",
            "Tinted With You",
            "Kissable Lips",
            "Peach of Time",
            "Light On Me"
        )
        val title = this.selectFirst("a.list-title")?.text() ?: return null
        if (boyslove.any { title.contains(it, ignoreCase = true) }) {
            return null
        }
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.media.media-cover")?.attr("data-src"))
        val blDisla = this.selectFirst("div.col a.list-category")?.text() ?: return null
        val tvType = if (href.contains("/film/")) {
            TvType.Movie
        } else if (href.contains("/anime/")) {
            TvType.Anime
        } else {
            TvType.AsianDrama
        }
        if (blDisla.contains("BL", ignoreCase = false)) {
            return null
        }
        return newMovieSearchResponse(title, href, type = tvType) {
            this.posterUrl = posterUrl
        }
    }
    private fun Element.toLatestEpisodeResult(): SearchResponse? {
        val title = this.selectFirst("div.list-title")?.text() ?: return null
        
        val originalHref = this.selectFirst("a")?.attr("href") ?: return null
        
        
        val href = fixUrlNull(originalHref.replace(Regex("/\\d+-sezon/\\d+-bolum$"), "/"))
        
        
        var posterUrl: String? = null
        
        
        this.selectFirst("div.media.media-episode")?.attr("style")?.let { styleAttr ->
            val decodedStyle = styleAttr.replace("&quot;", "\"")
            val regex = """url\("([^"]+)"\)""".toRegex()
            posterUrl = regex.find(decodedStyle)?.groupValues?.get(1)
        }
        
        
        if (posterUrl.isNullOrEmpty()) {
            posterUrl = this.selectFirst("div.media.media-episode")?.attr("data-src")
        }
        
        
        if (posterUrl.isNullOrEmpty()) {
            this.selectFirst("div.media.media-episode")?.attr("style")?.let { style ->
                val urlRegex = """https://[^"'\s)]+\.(jpg|jpeg|png|webp)""".toRegex(RegexOption.IGNORE_CASE)
                posterUrl = urlRegex.find(style)?.value
            }
        }
        
        val episodeInfo = this.selectFirst("div.list-category")?.text() ?: ""
        
        
        val fullTitle = "$title - $episodeInfo"
        
        val tvType = if (href?.contains("/film/") == true) {
            TvType.Movie
        } else if (href?.contains("/anime/") == true) {
            TvType.Anime
        } else {
            TvType.AsianDrama
        }
        
        return newMovieSearchResponse(fullTitle, href ?: return null, type = tvType) {
            this.posterUrl = fixUrlNull(posterUrl)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/arama/${query}").document

        return document.select("div.col").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("a.list-title")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.media.media-cover")?.attr("data-src"))
        val tvType    = if (href.contains("/film/")) {
            TvType.Movie
        } else if (href.contains("/anime/")){
            TvType.Anime
        }
        else {
            TvType.AsianDrama
        }

        return newTvSeriesSearchResponse(title, href, type = tvType) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("div.media.media-cover")?.attr("data-src"))
        val description     = document.selectFirst("div.text-content")?.text()?.trim()
        val movDescription  = document.selectFirst("div.video-attr:nth-child(4) > div:nth-child(2)")?.text()?.trim()
        val year            = document.selectFirst("div.featured-attr:nth-child(1) > div:nth-child(2)")?.text()?.trim()?.toIntOrNull()
        val movYear         = document.selectFirst("div.video-attr:nth-child(3) > div:nth-child(2)")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.categories a").map { it.text() }
        val movTags         = document.select("div.category a").map { it.text() }
        val actors          = document.select("span.valor a").map { Actor(it.text()) }
        val recommendations = document.select("div.col").mapNotNull { it.toRecommendationResult() }
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }
        val bolumler        = document.select("div.episodes a").map { bolum ->
            val bHref       = fixUrlNull(bolum.attr("href"))
            val bNum        = bolum.selectFirst("div.episode")?.text()?.substringBefore(".")?.toIntOrNull()
            val bSeason     = bHref?.substringBefore("-sezon")?.substringAfterLast("/")?.toIntOrNull()
            newEpisode(bHref, {
                this.episode = bNum
                this.season  = bSeason
                this.name    = "Bölüm"
                this.posterUrl = poster
            })
        }
        return if (url.contains("/film/")) {
         newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl       = poster
            this.plot            = movDescription
            this.year            = movYear
            this.tags            = movTags
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    } else if (url.contains("/anime/")) {
            newAnimeLoadResponse(title, url, TvType.Anime, true) {
                this.posterUrl       = poster
                this.plot            = description
                this.year            = year
                this.tags            = tags
                this.recommendations = recommendations
                this.episodes        = mutableMapOf(DubStatus.Subbed to bolumler)
                addTrailer(trailer)
            }
    }else {
            newTvSeriesLoadResponse(title, url, TvType.AsianDrama, bolumler) {
                this.posterUrl       = poster
                this.plot            = description
                this.year            = year
                this.tags            = tags
                this.recommendations = recommendations
                addTrailer(trailer)
            }
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("a.list-title")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.media.media-cover")?.attr("data-src"))
        val tvType    = if (href.contains("/film/")) {
            TvType.Movie
        } else if (href.contains("/anime/")){
            TvType.Anime
        }
        else {
            TvType.AsianDrama
        }

        return newMovieSearchResponse(title, href, type = tvType) { this.posterUrl = posterUrl }
    }


    override suspend fun loadLinks(
    data: String, 
    isCasting: Boolean, 
    subtitleCallback: (SubtitleFile) -> Unit, 
    callback: (ExtractorLink) -> Unit
): Boolean {
    val document = app.get(data).document
    
    val embedIds = document.select("[data-embed]")
        .mapNotNull { it.attr("data-embed") }
        .distinct()
    
    for (id in embedIds) {
        val response = app.post(
            url = "$mainUrl/ajax/embed",
            referer = data,
            headers = mapOf(
                "X-Requested-With" to "XMLHttpRequest",
                "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
            ),
            data = mapOf("id" to id)
        ).document
        
        val iframe = response.selectFirst("iframe")?.attr("src").toString()
        val iframeGet = app.get(iframe, referer = data).document
        val iframeSon = iframeGet.selectFirst("iframe")?.attr("src").toString()
        
        Log.d("kraptor_$name", "iframeSon = $iframeSon")
        
        
        if (iframeSon.contains("webdrama.sbs/video/")) {
            handleWebDrama(iframeSon, data, callback)
        } else {
            loadExtractor(iframeSon, "$mainUrl/", subtitleCallback, callback)
        }
    }
    return true
}

private suspend fun handleWebDrama(iframeSon: String, referer: String, callback: (ExtractorLink) -> Unit) {
    try {
        
        val videoId = iframeSon.substringAfter("webdrama.sbs/video/")
        Log.d("kerimmkirac", "WebDrama videoId = $videoId")
        
        
        val iframeResponse = app.get(
            url = iframeSon,
            referer = referer
        )
        
        
        val cookies = iframeResponse.cookies
        val fireplayerCookie = cookies["fireplayer_player"] ?: "6qgq1bmrp7gisci61s2p7edgrr"
        
        Log.d("kerimmkirac", "WebDrama cookie = $fireplayerCookie")
        
        
        val response = app.post(
            url = "https://webdrama.sbs/player/index.php?data=$videoId&do=getVideo",
            referer = iframeSon,
            headers = mapOf(
                "X-Requested-With" to "XMLHttpRequest",
                "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
                "Accept" to "*/*",
                "Origin" to "https://webdrama.sbs",
                "Sec-Fetch-Site" to "same-origin",
                "Sec-Fetch-Mode" to "cors",
                "Sec-Fetch-Dest" to "empty",
                "Cookie" to "fireplayer_player=$fireplayerCookie"
            )
        )
        
        val jsonResponse = response.parsedSafe<WebDramaResponse>()
        
        jsonResponse?.let { webDrama ->
            
            webDrama.videoSource?.let { videoSource ->
                callback.invoke(
                    newExtractorLink(
                        "WDT 1",
                        "WDT 1",
                        videoSource,
                        type = if (videoSource.contains(".m3u8") || videoSource.contains(".txt")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                    ) {
                        this.referer = "https://webdrama.sbs/"
                        this.headers = mapOf(
                            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
                            "Accept" to "*/*",
                            "Sec-Ch-Ua" to "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Brave\";v=\"138\"",
                            "Sec-Ch-Ua-Mobile" to "?0",
                            "Sec-Ch-Ua-Platform" to "\"Windows\"",
                            "Sec-Fetch-Site" to "same-origin",
                            "Sec-Fetch-Mode" to "cors",
                            "Sec-Fetch-Dest" to "empty",
                            "Accept-Language" to "tr-TR,tr;q=0.6",
                            "Accept-Encoding" to "gzip, deflate, br, zstd",
                            "Cookie" to "fireplayer_player=$fireplayerCookie"
                        )
                    }
                )
            }
            
            
            webDrama.securedLink?.let { securedLink ->
                callback.invoke(
                    newExtractorLink(
                        "WDT 2",
                        "WDT 2",
                        securedLink,
                        type = ExtractorLinkType.M3U8
                    ) {
                        this.referer = "https://webdrama.sbs/"
                        this.headers = mapOf(
                            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
                            "Accept" to "*/*",
                            "Sec-Ch-Ua" to "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Brave\";v=\"138\"",
                            "Sec-Ch-Ua-Mobile" to "?0",
                            "Sec-Ch-Ua-Platform" to "\"Windows\"",
                            "Sec-Fetch-Site" to "same-origin",
                            "Sec-Fetch-Mode" to "cors",
                            "Sec-Fetch-Dest" to "empty",
                            "Accept-Language" to "tr-TR,tr;q=0.6",
                            "Accept-Encoding" to "gzip, deflate, br, zstd",
                            "Cookie" to "fireplayer_player=$fireplayerCookie"
                        )
                    }
                )
            }
        }
        
    } catch (e: Exception) {
        Log.e("kerimmkirac", "WebDrama error: ${e.message}")
    }
}


data class WebDramaResponse(
    val hls: Boolean? = null,
    val videoImage: String? = null,
    val videoSource: String? = null,
    val securedLink: String? = null,
    val downloadLinks: List<String>? = null,
    val attachmentLinks: List<String>? = null,
    val ck: String? = null
)}