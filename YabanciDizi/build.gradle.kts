version = 2

cloudstream {
    authors     = listOf("nikyokki")
    language    = "tr"
    description = "Son çıkan yabancı dizi ve filmleri yabancidizi' de izle. En yeni yabancı film ve diziler, türkçe altyazılı yada dublaj olarak 1080p kalitesinde hd izle."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("TvSeries")
    iconUrl = "https://www.google.com/s2/favicons?domain=yabancidizi.tv&sz=%size%"
}
