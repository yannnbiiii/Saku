package com.example.util

import com.example.data.model.Expense
import com.example.ui.util.CsvExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    @Test
    fun testGenerateCsvEmptyList() {
        val csv = CsvExporter.generateCsv(emptyList())
        assertTrue("CSV should start with UTF-8 BOM", csv.startsWith('\uFEFF'))
        assertTrue("CSV should contain header", csv.contains("ID,Tanggal,Keterangan,Kategori,Nominal_IDR,Catatan"))
    }

    @Test
    fun testGenerateCsvWithExpensesAndEscaping() {
        val expenses = listOf(
            Expense(
                id = "exp1",
                date = "2026-11-08",
                title = "Makan Siang \"Nasi Padang\", Murah",
                categoryName = "Makanan",
                amount = 35000.0,
                notes = "Ayam gulai, es teh"
            ),
            Expense(
                id = "exp2",
                date = "2026-11-09",
                title = "Bensin Motor",
                categoryName = "Transportasi",
                amount = 50000.0,
                notes = ""
            )
        )

        val csv = CsvExporter.generateCsv(expenses)

        assertTrue(csv.contains("Makan Siang \"\"Nasi Padang\"\", Murah"))
        assertTrue(csv.contains("35000"))
        assertTrue(csv.contains("Bensin Motor"))
        assertTrue(csv.contains("50000"))
        assertTrue(csv.contains("Transportasi"))
    }
}
