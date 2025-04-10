version = 2

cloudstream {
    authors     = listOf("nikyokki")
    language    = "tr"
    description = "En güncel ve popüler filmleri Full HD kalitesinde, Türkçe altyazılı veya Türkçe dublaj seçenekleriyle izleyebileceğiniz tek adres! Sinema tutkunları için en özel film deneyimini yaşamak için fullhdfilmizlede.net"

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie")
    iconUrl = "https://www.google.com/s2/favicons?domain=fullhdfilmizlede.net&sz=%size%"
}