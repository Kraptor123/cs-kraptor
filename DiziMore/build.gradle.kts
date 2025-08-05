// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
version = 12

cloudstream {
    authors     = listOf("kraptor","kerimmkirac")
    language    = "tr"
    description = "Sadece Dizi izlemenin Tek Adresin yerli yabancı dizi izlemek için dizimore.com"

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("TvSeries")
    iconUrl = "https://t0.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://dizimore.com&size=64"
}