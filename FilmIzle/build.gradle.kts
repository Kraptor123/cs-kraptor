// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
version = 13

cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "Filmizle ile Full HD Film izle. Yerli ve yabancı filmleri Türkçe dublaj veya Alt yazılı seçeneğiyle kesintisiz ve sorunsuz en hızlı film sitesi."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie")
    iconUrl = "https://filmizle.cx/favicon.ico"
}