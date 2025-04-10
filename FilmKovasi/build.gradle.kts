version = 3

cloudstream {
    authors     = listOf("nikyokki")
    language    = "tr"
    description = "Film Kovasi ⚡ ile en güncel ve sorunsuz Full HD Film izle keyfi her yerde seninle! 1080p online film izleme ayrıcalığıyla Film Kovası'nın Tadını Çıkarın."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie")
    iconUrl = "https://www.google.com/s2/favicons?domain=filmkovasi.tv&sz=%size%"
}