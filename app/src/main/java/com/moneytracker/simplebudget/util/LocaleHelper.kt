package com.moneytracker.simplebudget.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    fun wrapContext(context: Context, languageCode: String): Context {
        // Resource locale — must match the values-XX resource folder.
        val resourceLocale = Locale.forLanguageTag(languageCode)

        // Default locale used by date/number formatters (DateTimeFormatter, etc.).
        // Serbian resources use Cyrillic as the default script, but the app is
        // written in Latin, so we point formatters at sr-Latn so month names
        // come out in Latin script instead of Cyrillic.
        val defaultLocale = if (languageCode == "sr")
            Locale.forLanguageTag("sr-Latn")
        else
            resourceLocale

        Locale.setDefault(defaultLocale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(resourceLocale)
        return context.createConfigurationContext(config)
    }
}
