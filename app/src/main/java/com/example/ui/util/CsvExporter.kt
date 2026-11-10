package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Expense
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object CsvExporter {

    /**
     * Converts a list of Expense items into RFC 4180 compliant CSV string.
     * Includes UTF-8 BOM so Excel opens special characters and text accurately.
     */
    fun generateCsv(expenses: List<Expense>): String {
        val sb = StringBuilder()
        // UTF-8 Byte Order Mark (BOM) for Excel compatibility
        sb.append('\uFEFF')

        // CSV Header
        sb.append("ID,Tanggal,Keterangan,Kategori,Nominal_IDR,Catatan\n")

        for (exp in expenses) {
            val safeId = escapeCsv(exp.id)
            val safeDate = escapeCsv(exp.date)
            val safeTitle = escapeCsv(exp.title)
            val safeCategory = escapeCsv(exp.categoryName)
            val safeAmount = exp.amount.toLong().toString()
            val safeNotes = escapeCsv(exp.notes)

            sb.append("$safeId,$safeDate,$safeTitle,$safeCategory,$safeAmount,$safeNotes\n")
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        val containsSpecial = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        val escaped = value.replace("\"", "\"\"")
        return if (containsSpecial) {
            "\"$escaped\""
        } else {
            "\"$escaped\""
        }
    }

    /**
     * Writes CSV string directly into the user selected Storage Access Framework URI.
     */
    fun writeToUri(context: Context, uri: Uri, csvContent: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
                    writer.write(csvContent)
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Saves CSV into cache directory and prepares an ACTION_SEND Intent.
     */
    fun createShareIntent(context: Context, filename: String, csvContent: String): Intent {
        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.write(csvContent)
                writer.flush()
            }
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan Saku - $filename")
            putExtra(Intent.EXTRA_TEXT, "Berikut terlampir laporan pengeluaran keuangan pribadi dari aplikasi Saku.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return Intent.createChooser(intent, "Bagikan Laporan CSV")
    }
}
