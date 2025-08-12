version = 26

cloudstream {
    authors     = listOf("keyiflerolsun")
    language    = "tr"
    description = "Binlerce yerli yabancı dizi arşivi, tüm sezonlar, kesintisiz bölümler. Sadece dizi izle, Dizimom heryerde seninle!"

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("TvSeries")
    iconUrl = "https://t1.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://www.dizimom.mom&size=32"
}
