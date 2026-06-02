package de.streamonkey.teamswear.ui.util

import android.text.format.DateUtils
import java.time.Instant

/** ISO-8601 -> relative Zeit in der Geraete-Locale ("vor 5 Min." / "5 min ago"). Leer bei Fehler. */
fun relativeTime(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        val ms = Instant.parse(iso).toEpochMilli()
        DateUtils.getRelativeTimeSpanString(
            ms, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
        ).toString()
    } catch (_: Exception) {
        ""
    }
}
