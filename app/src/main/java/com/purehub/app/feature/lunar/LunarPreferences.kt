package com.purehub.app.feature.lunar

import android.content.Context

object LunarPreferences {
    private const val FILE = "purehub_lunar_preferences"
    private const val REGION = "region"

    fun region(context: Context): LunarRegion = runCatching {
        LunarRegion.valueOf(context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(REGION, null).orEmpty())
    }.getOrDefault(LunarRegion.Vietnam)

    fun setRegion(context: Context, region: LunarRegion) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(REGION, region.name).apply()
    }
}
