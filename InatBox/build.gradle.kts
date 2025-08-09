version = 24

cloudstream {
    authors     = listOf("JustRelaxable", "keyiflerolsun", "kraptor")
    language    = "tr"
    description = "İnatBox cloudstream eklentisidir. Sevdiğiniz yayın platformlarını ve canlı maçları burada bulabilirsiniz."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 3 // will be 3 if unspecified
    tvTypes = listOf("Movie", "TvSeries", "Live")
    iconUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEh3vCp6N1K4bECoYRQD-cisJF2_6V_Hk01ZhDmoPR2JuM8O5qr4MqrPO1munM9cRlleBBSK6odYhLtDBWv4E3vhPhynlmS5hVVtJZShHoGA5REQ8_3v8SIlccTEqzVQu2UJyNYQdJNrKIfWy66RQeT0D-CcmFCbHPz5023H6p2v5fv4NVloZ5Rqo_yGrIY/s320/iNat-Box-App.png"
}