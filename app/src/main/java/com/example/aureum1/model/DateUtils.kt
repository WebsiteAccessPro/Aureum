package com.example.aureum1.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utilidades de fecha usadas por la vista para mostrar etiquetas amigables.
 */
object DateUtils {
    fun relativeDate(fechaStr: String, createdAt: Any?): String {
        val today = Calendar.getInstance()
        val formats = listOf(
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        )
        var date: Date? = null
        for (f in formats) {
            try { date = f.parse(fechaStr); if (date != null) break } catch (_: Exception) {}
        }
        if (date == null && createdAt is com.google.firebase.Timestamp) {
            date = createdAt.toDate()
        }
        if (date == null) return fechaStr.ifBlank { "Hoy" }

        val cal = Calendar.getInstance().apply { time = date!! }
        val isSameDay = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        if (isSameDay) return "Hoy"

        val ayer = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = cal.get(Calendar.YEAR) == ayer.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == ayer.get(Calendar.DAY_OF_YEAR)
        if (isYesterday) return "Ayer"

        return formats.first().format(date!!)
    }

    fun monthLabel(fechaStr: String, createdAt: Any?): String {
        val formats = listOf(
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        )
        var date: Date? = null
        for (f in formats) {
            try { date = f.parse(fechaStr); if (date != null) break } catch (_: Exception) {}
        }
        if (date == null && createdAt is com.google.firebase.Timestamp) {
            date = createdAt.toDate()
        }
        val d = date ?: Date()
        val localeEs = Locale("es", "ES")
        val fmt = SimpleDateFormat("MMMM yyyy", localeEs)
        val label = fmt.format(d)
        return label.replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeEs) else it.toString() }
    }
}