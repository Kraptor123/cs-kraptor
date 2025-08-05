version = 6

cloudstream {
    authors     = listOf("kerimmkirac")
    language    = "tr"
    description = "Asya'nın en iyi Çin ve Kore dizilerini ve filmlerini keşfedin! Romantizm, dram, aksiyon... Diziasya ile Asya kültürüne hemen adım atın!"

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("AsianDrama","Movie","Anime")
    iconUrl = "https://www.google.com/s2/favicons?domain=https://www.diziasya.com/&sz=%size%"
}
