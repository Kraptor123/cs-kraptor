// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
version = 1

cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "Yabancı dizi izle, anime izle, en popüler yabancı dizileri ve animeleri ücretsiz olarak diziwatch.tv'de izleyin."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("TvSeries", "Anime") //Movie, AnimeMovie, TvSeries, Cartoon, Anime, OVA, Torrent, Documentary, AsianDrama, Live, NSFW, Others, Music, AudioBook, CustomMedia, Audio, Podcast,
    iconUrl = "https://www.google.com/s2/favicons?domain=www.setfilmizle.de&sz=%size%"
}