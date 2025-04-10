version = 8

cloudstream {
    authors     = listOf("keyiflerolsun", "usdogu", "nikyokki")
    language    = "tr"
    description = "Film Makinesi, en yeni ve en güncel filmleri sitemizde full HD kalite farkı ile izleyebilirsiniz. HD film izle denildiğinde akla gelen en kaliteli film izleme sitesi."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie")
    iconUrl = "https://www.google.com/s2/favicons?domain=filmmakinesi.film&sz=%size%"
}
