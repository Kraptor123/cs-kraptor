// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
version = 3

cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "Hint filmleri, Hint filmleri izle, Asya filmleri izle, Kore filmleri izle, HD izle, Full izle, 1080p izle, online izle, Tek parça izle."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("AsianDrama") //Movie, AnimeMovie, TvSeries, Cartoon, Anime, OVA, Torrent, Documentary, AsianDrama, Live, NSFW, Others, Music, AudioBook, CustomMedia, Audio, Podcast,
    iconUrl = "https://t1.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://asyafilmizlesene.org&size=128"
}