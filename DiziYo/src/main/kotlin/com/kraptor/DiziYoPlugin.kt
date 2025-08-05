package com.kraptor

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class DiziYoPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(DiziYo())
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
        registerExtractorAPI(VidMolyExtractor())
        registerExtractorAPI(VidMolyTo())
    }
}