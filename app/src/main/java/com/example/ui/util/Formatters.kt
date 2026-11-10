package com.example.ui.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object Formatters {

    private val idLocale = Locale.forLanguageTag("id-ID")

    fun formatRupiah(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(idLocale)
        format.maximumFractionDigits = 0
        return format.format(amount).replace("Rp", "Rp ")
    }

    fun currentMonth(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return sdf.format(Calendar.getInstance().time)
    }

    fun todayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Calendar.getInstance().time)
    }

    fun formatMonthDisplay(monthStr: String): String {
        return try {
            val sdfInput = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val date = sdfInput.parse(monthStr) ?: return monthStr
            val sdfOutput = SimpleDateFormat("MMMM yyyy", idLocale)
            sdfOutput.format(date)
        } catch (e: Exception) {
            monthStr
        }
    }

    fun formatDateDisplay(dateStr: String): String {
        return try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdfInput.parse(dateStr) ?: return dateStr
            val sdfOutput = SimpleDateFormat("d MMM yyyy", idLocale)
            sdfOutput.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }
}
