// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
version = 9

cloudstream {
    authors     = listOf("kraptor","kerimmkirac")
    language    = "tr"
    description = "Web Drama Turkey - Kore, Çin, Japon Dizileri, Filmleri ve Animeler"

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("AsianDrama","Movie","Anime") //Movie, AnimeMovie, TvSeries, Cartoon, Anime, OVA, Torrent, Documentary, AsianDrama, Live, NSFW, Others, Music, AudioBook, CustomMedia, Audio, Podcast,
    iconUrl = "https://t2.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://webdramaturkey.org/&size=40"
}