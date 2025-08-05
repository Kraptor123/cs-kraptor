// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
version = 5

cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "En sevilen Hint dizileri ve filmleri, güncel içerikler ve arşivlik yapımlar HintDizi.com'da! Dizi arşivimize göz atın ve favori yapımlarınızı kaçırmayın."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("AsianDrama") //Movie, AnimeMovie, TvSeries, Cartoon, Anime, OVA, Torrent, Documentary, AsianDrama, Live, NSFW, Others, Music, AudioBook, CustomMedia, Audio, Podcast,
    iconUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://hintdizi.com&size=128"
}