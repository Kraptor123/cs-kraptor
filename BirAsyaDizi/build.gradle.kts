// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
version = 8

cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "Asya'nın En Ünlü Dizilerini en yüksek çözünürlükte HD ve kesintisiz izleyin, kaliteli Asya film ve dizilerinin keyfini sürün."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("AsianDrama")
    iconUrl = "https://t1.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://www.birasyadizi.com/&size=16"
}