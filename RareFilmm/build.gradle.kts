version = 2

cloudstream {
    authors     = listOf("keyiflerolsun")
    language    = "en"
    description = "The Cave of Forgotten Films"

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie")
    iconUrl = "https://www.google.com/s2/favicons?domain=rarefilmm.com&sz=%size%"
}