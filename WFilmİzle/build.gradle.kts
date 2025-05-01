version = 5

cloudstream {
    authors     = listOf("nikyokki", "kerimmkirac")
    language    = "tr"
    description = "Aile Filmleri izle, Yerli ve yabancı en yeni Aile filmlerini Full HD 1080p görüntü kalitesiyle Türkçe Dublaj ve Türkçe Altyazı seçenekleriyle izleyin."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie")
    iconUrl = "https://www.google.com/s2/favicons?domain=wfilmizle.my&sz=%size%"
}
