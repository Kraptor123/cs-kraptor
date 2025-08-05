version = 7

cloudstream {
    authors     = listOf("kerimmkirac")
    language    = "tr"
    description = "Yeni diziwatch yani Yeniwatch. Yabancı dizi izle, anime izle, en popüler yabancı dizileri ve animeleri ücretsiz olarak yeniwatch.net.tr'te izleyin."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Anime")
    iconUrl = "https://www.google.com/s2/favicons?domain=yeniwatch.net.tr&sz=%size%"
}
