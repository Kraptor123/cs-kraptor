// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
version = 16

cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "Yabancı film izle olarak vizyondaki en yeni yabancı filmleri türkçe dublaj ve altyazılı olarak en hızlı şekilde full hd olarak sizlere sunuyoruz."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie", "TvSeries")
    iconUrl = "https://t2.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://sinefy3.com/&size=128"
}