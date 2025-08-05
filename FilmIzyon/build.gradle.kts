// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
version = 6

cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "Yerli ve yabancı ödüllü tüm filmleri Türkçe Dublaj veya Orjinal dilde izleyebileceğiniz Türkiye'nin en kaliteli film izleme platformuna hoşgeldiniz."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie")
    iconUrl = "https://t2.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://www.filmizyon.com/&size=64"
}