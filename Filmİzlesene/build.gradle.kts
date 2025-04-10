version = 3

cloudstream {
    authors     = listOf("nikyokki")
    language    = "tr"
    description = "Filmizlesene ile hızlı film izleme fırsatını yakala, en yeni ve iyi filmleri Full HD 1080p kalitesiyle online ve bedava izle"

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie")
    iconUrl = "https://www.google.com/s2/favicons?domain=filmizlesene.plus&sz=%size%"
}