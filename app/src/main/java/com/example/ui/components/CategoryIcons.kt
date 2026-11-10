package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIcons {

    val availableIcons = listOf(
        "restaurant" to Icons.Default.Restaurant,
        "commute" to Icons.Default.DirectionsCar,
        "movie" to Icons.Default.Movie,
        "receipt" to Icons.Default.Receipt,
        "shopping_bag" to Icons.Default.ShoppingBag,
        "medical" to Icons.Default.LocalHospital,
        "school" to Icons.Default.School,
        "flight" to Icons.Default.Flight,
        "home" to Icons.Default.Home,
        "fitness" to Icons.Default.FitnessCenter,
        "savings" to Icons.Default.Savings,
        "category" to Icons.Default.Category
    )

    // Unified single color for consistency across all categories
    val availableColors = listOf(
        "#10B981"
    )

    fun getIcon(name: String): ImageVector {
        return availableIcons.firstOrNull { it.first == name }?.second ?: Icons.Default.Category
    }

    fun parseColor(hex: String, fallback: Color = Color(0xFF10B981)): Color {
        return Color(0xFF10B981) // Uniform unified color for consistent category styling
    }
}
