version = 6

cloudstream {
    authors     = listOf("keyiflerolsun")
    language    = "tr"
    description = "ÇizgiMax ile Çizgi Film izlemek artık daha kolay, donmadan full hd ve reklamsız bir sitedir, içerisinde 700 den fazla çizgi film olan, Bu site bu işi profesyonelce yapıyor."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Cartoon")
    iconUrl = "https://www.google.com/s2/favicons?domain=cizgimax.online&sz=%size%"
}