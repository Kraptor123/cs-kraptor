// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
version = 4

cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "Ddizi, dizi izle, dizi seyret, yerli dizi izle, canlı dizi, türk dizi izle, dizi izle full, diziizle, eski diziler."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("TvSeries") //Movie, AnimeMovie, TvSeries, Cartoon, Anime, OVA, Torrent, Documentary, AsianDrama, Live, NSFW, Others, Music, AudioBook, CustomMedia, Audio, Podcast,
    iconUrl = "https://www.ddizi.im/images/logo.png"
}