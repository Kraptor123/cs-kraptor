version = 18

cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "Her Dizi Bir Macera, Her An Bir Keşif!"

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("TvSeries", "Movie")
    iconUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://dizifun4.com&size=128"
}