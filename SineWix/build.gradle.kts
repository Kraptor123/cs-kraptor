// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
version = 7
cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "SineWix - Ücretsiz Film, Dizi ve Anime İzleme Uygulaması 5000+ HD İçerik, Türkçe Altyazılı, Otaku Dostu Anime Arşivi, Premium Kalite Yabancı Yapımlar"

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie", "TvSeries", "Anime", "AsianDrama", "Cartoon") //Movie, AnimeMovie, TvSeries, Cartoon, Anime, OVA, Torrent, Documentary, AsianDrama, Live, NSFW, Others, Music, AudioBook, CustomMedia, Audio, Podcast,
    iconUrl = "https://play-lh.googleusercontent.com/brwGNmr7IjA_MKk_TTPs0va10hdKE_bD_a1lnKoiMuCayW98EHpRv55edA6aEoJlmwfX=w240-h480"
}