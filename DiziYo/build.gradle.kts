version = 2

cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "Diziyo ile Yabancı Dizi, Film, Anime, Çizgi-Film izle."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("TvSeries", "Movie", "Anime")
    iconUrl = "https://www.diziyo.de/wp-content/uploads/2019/11/favicon-150x150.ico"
}