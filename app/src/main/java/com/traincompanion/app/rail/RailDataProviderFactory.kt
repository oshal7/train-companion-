package com.traincompanion.app.rail

import com.traincompanion.app.data.AppSettings
import com.traincompanion.app.data.RailProviderType

object RailDataProviderFactory {
    fun create(settings: AppSettings): RailDataProvider = when (settings.providerType) {
        RailProviderType.MOCK -> MockRailDataProvider()
        RailProviderType.COMMUNITY_PNR -> CommunityPnrProvider()
        RailProviderType.RAILWAY_API, RailProviderType.INDIAN_RAIL_API ->
            if (settings.apiKey.isBlank()) {
                MockRailDataProvider()
            } else {
                RailApiProvider(settings.providerType, settings.apiKey)
            }
    }
}
