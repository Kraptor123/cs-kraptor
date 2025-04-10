version = 3

cloudstream {
    authors     = listOf("nikyokki")
    language    = "tr"
    description = "Türkçe altyazılı film izle, full hd olarak film izlemenin keyfini çıkarın. Türkiyenin en büyük film izleme platformu tafdi kalitesiyle sizlerle."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie")
    iconUrl = "https://www.google.com/s2/favicons?domain=tafdi.info&sz=%size%"
}