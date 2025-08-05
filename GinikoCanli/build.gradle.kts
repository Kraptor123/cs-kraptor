// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
version = 5

cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "Vpn Gerekli! Dünyanın tüm ülkelerinden ücretsiz çevrimiçi TV."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Live")
    iconUrl = "https://www.giniko.com/images/favicon.ico"
}