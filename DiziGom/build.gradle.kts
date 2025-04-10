version = 5

cloudstream {
    authors     = listOf("nikyokki")
    language    = "tr"
    description = "Türkçe altyazılı yabancı dizi izle, Tüm yabancı, kore, netflix dizilerin yeni ve eski sezonlarını orijinal dilinde dizigom1 alt yazılı film izleyebilir, sadece türkçe altyazılı en iyi yabancı diziler ve filmler hakkında yorum yapabilirsiniz."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie","TvSeries")
    iconUrl = "https://www.google.com/s2/favicons?domain=dizigom1.co&sz=%size%"
}