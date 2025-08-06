// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
version = 17
dependencies {
    implementation("androidx.annotation:annotation-jvm:1.9.1")
}

cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "Sade tasarıma sahip, yüksek kaliteli anime izleme sitesi. En sevdiğiniz anime serilerini, kesintisiz bir şekilde izleyebilirsiniz. Geniş arşivimiz sayesinde, her zaman yeni çıkan animeleri ve eski klasikleri keşfedebilirsiniz."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "AnimeMovie",
        "Anime",
        "Ova"
    )
    iconUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://www.animeizlesene.com&size=128"
}
