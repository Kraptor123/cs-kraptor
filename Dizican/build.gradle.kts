version = 7

cloudstream {
    authors     = listOf("kerimmkirac")
    language    = "tr"
    description = "DiziCan TV'de en popüler Kore, Çin ve Asya dizilerini keşfet! Güncel bölümler, kaliteli altyazılar ve sınırsız dizi keyfi seni bekliyor."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie","AsianDrama")
    iconUrl = "https://www.google.com/s2/favicons?domain=dizican.tv&sz=%size%"
}