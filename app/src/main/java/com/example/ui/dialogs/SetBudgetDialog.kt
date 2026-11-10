package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Budget
import com.example.data.model.Category
import com.example.ui.components.CategoryIcons
import com.example.ui.util.Formatters

@Composable
fun SetBudgetDialog(
    categories: List<Category>,
    month: String,
    existingBudget: Budget? = null,
    onDismiss: () -> Unit,
    onSave: (categoryId: String, categoryName: String, monthlyLimit: Double, month: String) -> Unit,
    onAddCategory: (name: String, iconName: String, colorHex: String) -> Unit = { _, _, _ -> }
) {
    val displayCategories = if (categories.isEmpty()) {
        listOf(
            Category(id = "fb_1", name = "Makanan & Minuman", iconName = "restaurant", colorHex = "#10B981"),
            Category(id = "fb_2", name = "Transportasi", iconName = "commute", colorHex = "#10B981"),
            Category(id = "fb_3", name = "Hiburan", iconName = "movie", colorHex = "#10B981"),
            Category(id = "fb_4", name = "Tagihan", iconName = "receipt", colorHex = "#10B981"),
            Category(id = "fb_5", name = "Belanja", iconName = "shopping_bag", colorHex = "#10B981"),
            Category(id = "fb_6", name = "Lain-lain", iconName = "category", colorHex = "#10B981")
        )
    } else {
        categories
    }

    var selectedCategory by remember {
        mutableStateOf(
            displayCategories.firstOrNull { it.id == existingBudget?.categoryId }
                ?: displayCategories.firstOrNull()
        )
    }
    var limitText by remember {
        mutableStateOf(
            if (existingBudget != null && existingBudget.monthlyLimit > 0) {
                existingBudget.monthlyLimit.toLong().toString()
            } else ""
        )
    }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showAddCategorySubDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("set_budget_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (existingBudget == null) "Tetapkan Anggaran" else "Ubah Anggaran",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Bulan: ${Formatters.formatMonthDisplay(month)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Select Category
                Text(
                    text = "Pilih Kategori (Terpilih: ${selectedCategory?.name ?: "Belum dipilih"})",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(displayCategories) { category ->
                        val isSelected = selectedCategory?.id == category.id || selectedCategory?.name.equals(category.name, ignoreCase = true)
                        val catColor = CategoryIcons.parseColor(category.colorHex)
                        val icon = CategoryIcons.getIcon(category.iconName)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) catColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) catColor else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(catColor.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = category.name,
                                        tint = catColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Add Category Quick Button Chip
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { showAddCategorySubDialog = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Tambah Kategori",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "+ Pilih Lainnya",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Monthly Limit Input
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            limitText = input
                            errorText = null
                        }
                    },
                    label = { Text("Batas Anggaran Bulanan (Rp)") },
                    placeholder = { Text("1000000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_limit_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick preset limits
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf(250000, 500000, 1000000, 2000000, 3000000)
                    items(presets) { p ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { limitText = p.toString() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = Formatters.formatRupiah(p.toDouble()).replace(",00", ""),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                errorText?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                    Button(
                        onClick = {
                            val limit = limitText.toDoubleOrNull()
                            if (limit == null || limit <= 0) {
                                errorText = "Masukkan batas nominal anggaran yang valid (> 0)"
                                return@Button
                            }
                            val cat = selectedCategory ?: displayCategories.firstOrNull()
                            if (cat == null) {
                                errorText = "Pilih kategori anggaran"
                                return@Button
                            }
                            onSave(
                                cat.id.ifBlank { "cat_${cat.name.hashCode()}" },
                                cat.name,
                                limit,
                                month
                            )
                        },
                        modifier = Modifier.testTag("save_budget_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Simpan Anggaran")
                    }
                }
            }
        }
    }

    if (showAddCategorySubDialog) {
        QuickCategoryDialog(
            existingCategoryNames = displayCategories.map { item -> item.name },
            onDismiss = { showAddCategorySubDialog = false },
            onSelectPreset = { name, iconName, colorHex ->
                val existing = displayCategories.firstOrNull { it.name.equals(name, ignoreCase = true) }
                if (existing != null) {
                    selectedCategory = existing
                } else {
                    onAddCategory(name, iconName, colorHex)
                    selectedCategory = Category(name = name, iconName = iconName, colorHex = colorHex)
                }
                errorText = null
                showAddCategorySubDialog = false
            }
        )
    }
}
