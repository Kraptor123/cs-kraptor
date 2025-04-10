version = 2

cloudstream {
    authors     = listOf("nikyokki")
    language    = "tr"
    description = "HDFilmizle ile Full HD izle, 1080p kalitede Yerli ve Yabancı filmlerle Türkçe dublaj veya Altyazı olarak kesintisiz film izlemenin tadını çıkarın."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie", "TvSeries")
    iconUrl = "https://www.google.com/s2/favicons?domain=hdfilmizle.to&sz=%size%"
}