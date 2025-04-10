version = 1

cloudstream {
    authors     = listOf("keyiflerolsun")
    language    = "en"
    description = "!!! YAPIM AŞAMASINDA !!! Watch2Movies is a Free Movies streaming site with zero ads. We let you watch movies online without having to register or paying, with over 10000 movies and TV-Series. You can also Download full movies from Watch2Movies and watch it later if you want."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 3 // will be 3 if unspecified
    tvTypes = listOf("Movie")
    iconUrl = "https://www.google.com/s2/favicons?domain=watch2movies.net&sz=%size%"
}