// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
version = 14

cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "Yıllar geçse bile izledikten sonraki etkisi hala süren o unutulmaz filmleri sizler için derliyoruz. Altyazılı film izle. Sadece En iyi filmler."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie","TvSeries") //Movie, AnimeMovie, TvSeries, Cartoon, Anime, OVA, Torrent, Documentary, AsianDrama, Live, NSFW, Others, Music, AudioBook, CustomMedia, Audio, Podcast,
    iconUrl = "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://unutulmazfilmler4.com/&size=64"
}