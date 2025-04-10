version = 6

cloudstream {
    authors     = listOf("nikyokki")
    language    = "tr"
    description = "Film izle, Hdfilmsitesi ile Online 1080p ve 4K kalite Full HD Filmlere Türkçe Dublaj ve Altyazılı olarak Reklamsız Kesintisiz HD Film izle."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie")
    iconUrl = "https://www.google.com/s2/favicons?domain=hdfilmsitesi.net&sz=%size%"
}