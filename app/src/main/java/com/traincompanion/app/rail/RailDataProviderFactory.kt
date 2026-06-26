package com.traincompanion.app.rail

import com.traincompanion.app.data.AppSettings
import com.traincompanion.app.data.RailProviderType

object RailDataProviderFactory {
    fun create(settings: AppSettings): RailDataProvider =
        if (settings.providerType == RailProviderType.MOCK || settings.apiKey.isBlank()) {
            MockRailDataProvider()
        } else {
            RailApiProvider(settings.providerType, settings.apiKey)
        }
}
