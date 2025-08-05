// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
version = 12

cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "DiziKRAL - ücretsiz yabancı dizi izle, film izle, anime izle."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("TvSeries")
    iconUrl = "https://t0.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://dizikral.nl/mofy/img/192x192_.png&size=128"
}