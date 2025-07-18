package com.keyiflerolsun

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class DizillaPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Dizilla())
        registerExtractorAPI(ContentX())
        registerExtractorAPI(Hotlinger())
        registerExtractorAPI(RapidVid())
        registerExtractorAPI(TRsTX())
        registerExtractorAPI(VidMoxy())
        registerExtractorAPI(Sobreatsesuyp())
        registerExtractorAPI(TurboImgz())
        registerExtractorAPI(TurkeyPlayer())
        registerExtractorAPI(Hotlinger())
        registerExtractorAPI(FourCX())
        registerExtractorAPI(PlayRu())
        registerExtractorAPI(FourPlayRu())
        registerExtractorAPI(FourPichive())
        registerExtractorAPI(Pichive())
        registerExtractorAPI(FourDplayer())
        registerExtractorAPI(SNDplayer())
        registerExtractorAPI(ORGDplayer())
        registerExtractorAPI(SnHotLinger())
        registerExtractorAPI(SNDPlayer74())
    }
}