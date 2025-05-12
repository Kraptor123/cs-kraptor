version = 7

cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "Ücretsiz bir şekilde çizgi dizi, dizi, anime ve daha fazlasını eski-yeni demeden izleyebilirsiniz."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Cartoon")
    iconUrl = "https://cizgivedizi.com/Logo.png"
}