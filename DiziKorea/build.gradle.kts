version = 12

cloudstream {
    authors     = listOf("kerimmkirac")
    language    = "tr"
    description = "Kore Dizileri izle, Dizikorea size en yeni ve güncel romantik komedi, okul tarzında ki Kore Asya Dizilerini Full HD Türkçe Altyazılı izleme şansı verir."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("AsianDrama","Movie")
    iconUrl = "https://www.google.com/s2/favicons?domain=dizikorea.pw&sz=%size%"
}
