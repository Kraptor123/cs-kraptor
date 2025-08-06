version = 30

cloudstream {
    authors     = listOf("kraptor")
    language    = "tr"
    description = "Türkçe Altyazılı Anime İzle."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // Will be 3 if unspecified
    tvTypes = listOf("Anime")
    iconUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSQLGJxL_3l-lZofrxkC-vpKFzHzyKW-Z0lYg&s"
}
