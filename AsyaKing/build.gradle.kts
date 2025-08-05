version = 5

cloudstream {
    authors     = listOf("kerimmkirac")
    language    = "tr"
    description = "Asya Dizi İzle, kore dizi izle, çin dizi izle, bl dizileri izle denilince akla gelen ilk site ; 1080p fullhd türkçe altyazılı olarak izleyin."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("AsianDrama")
    iconUrl = "https://www.google.com/s2/favicons?domain=www.asyaking.com&sz=%size%"
}