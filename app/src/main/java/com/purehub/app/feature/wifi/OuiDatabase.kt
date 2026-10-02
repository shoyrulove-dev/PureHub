package com.purehub.app.feature.wifi

import android.content.Context

/** Offline longest-prefix vendor lookup generated from IEEE MA-L, MA-M and MA-S public listings. */
internal object OuiDatabase {
    @Volatile private var assignments: Map<String, String>? = null

    fun lookup(context: Context, macAddress: String): String {
        val normalized = macAddress.replace(Regex("[^0-9A-Fa-f]"), "").uppercase()
        if (normalized.length < 12) return "Vendor unavailable"
        val firstOctet = normalized.take(2).toIntOrNull(16) ?: return "Vendor unavailable"
        if (firstOctet and 0x02 != 0) return "Private / randomized MAC"
        val data = assignments ?: synchronized(this) {
            assignments ?: load(context).also { assignments = it }
        }
        return sequenceOf(9, 7, 6).map { normalized.take(it) }.firstNotNullOfOrNull(data::get)
            ?: "Vendor not listed"
    }

    private fun load(context: Context): Map<String, String> = runCatching {
        context.assets.open("ieee_oui.tsv").bufferedReader().useLines { lines ->
            lines.filterNot { it.startsWith('#') || it.isBlank() }.mapNotNull { line ->
                val split = line.split('\t', limit = 2)
                split.takeIf { it.size == 2 }?.let { it[0] to it[1] }
            }.toMap()
        }
    }.getOrDefault(emptyMap())
}
