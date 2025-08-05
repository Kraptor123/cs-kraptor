// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
version = 8

cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "Film izle online platformunda, Full hd film izleme ayrıcalığını yaşayın. Türkçe Dublaj ve altyazılı filmleri kayıt derdi olmadan 720p veya 1080p izleyin."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie")
    iconUrl = "https://t2.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://fullhdizle.one/&size=48"
}