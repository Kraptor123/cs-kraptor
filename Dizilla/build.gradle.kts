version = 16

cloudstream {
    authors     = listOf("keyiflerolsun", "nikyokki")
    language    = "tr"
    description = "Dizilla tüm yabancı dizileri ücretsiz olarak Türkçe Dublaj ve altyazılı seçenekleri ile 1080P kalite izleyebileceğiniz yeni nesil yabancı dizi izleme siteniz."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("TvSeries")
    iconUrl = "https://www.google.com/s2/favicons?domain=dizilla.club&sz=%size%"
}