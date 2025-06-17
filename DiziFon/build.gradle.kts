// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
version = 3

cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "Asya dizileri izle adresine hoş geldiniz. Dizifon olarak, Kore Dizileri, Çin dizileri, Hint dizileri, Japon dizileri gibi Asya’nın popüler yapımlarını Türkçe altyazılı olarak sizlerle buluşturuyoruz."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("AsianDrama")
    iconUrl = "https://t1.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://www.dizifon.com&size=128"
}