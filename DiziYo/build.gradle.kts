version = 24

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
    iconUrl = "https://icons.iconarchive.com/icons/yohproject/crayon-cute/256/movies-icon.png"
}