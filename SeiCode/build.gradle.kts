version = 5

cloudstream {
    authors     = listOf("kerimmkirac","kraptor")
    language    = "tr"
    description = "Seicode ile en yeni animeler keşfedin!"

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Anime","Movie")
    iconUrl = "https://www.google.com/s2/favicons?domain=seicode.net&sz=%size%"
}