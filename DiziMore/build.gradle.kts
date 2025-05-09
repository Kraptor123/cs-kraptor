// ! Bu araç @Kraptor123 tarafından | @kekikanime için yazılmıştır.
version = 1

cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "Sadece Dizi izlemenin Tek Adresin yerli yabancı dizi izlemek için dizimore.com"

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("TvSeries")
    iconUrl = "view-source:https://dizimore.com/wp-content/uploads/2024/03/morefav.png"
}