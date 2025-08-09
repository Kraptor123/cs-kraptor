version = 16

cloudstream {
    authors     = listOf("kraptor","kerimmkirac")
    language    = "tr"
    description = "Yabancı film izle, Türkçe dublaj ve Türkçe altyazılı film seçenekleriyle 720p ve 1080p HD kalitesinde film izle - Uğur Film full hd film izle."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie")
    iconUrl = "https://t2.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://ugurfilm9.com&size=128"
}
