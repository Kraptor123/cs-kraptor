// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
version = 6

cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "Asyaminik - En Güncel Asya Dizileri Burada!"

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("AsianDrama") //Movie, AnimeMovie, TvSeries, Cartoon, Anime, OVA, Torrent, Documentary, AsianDrama, Live, NSFW, Others, Music, AudioBook, CustomMedia, Audio, Podcast,
    iconUrl = "https://t0.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://asyaminik.com/&size=128"
}