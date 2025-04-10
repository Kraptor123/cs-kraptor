version = 12

cloudstream {
    authors     = listOf("keyiflerolsun", "yusiqo")
    language    = "tr"
    description = "Sinewix | Ücretsiz Film - Dizi - Anime İzleme Uygulaması."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie", "TvSeries", "Anime")
    iconUrl = "https://play-lh.googleusercontent.com/brwGNmr7IjA_MKk_TTPs0va10hdKE_bD_a1lnKoiMuCayW98EHpRv55edA6aEoJlmwfX"
}