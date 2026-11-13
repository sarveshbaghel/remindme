package com.taskly.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {
    fun getIcon(name: String): ImageVector {
        return when (name.lowercase()) {
            "person", "personal" -> Icons.Default.Person
            "work" -> Icons.Default.Work
            "school", "college", "education" -> Icons.Default.School
            "folder", "projects", "project" -> Icons.Default.Folder
            "fitnesscenter", "health", "fitness" -> Icons.Default.FitnessCenter
            "shoppingcart", "shopping" -> Icons.Default.ShoppingCart
            "home" -> Icons.Default.Home
            "book", "study" -> Icons.Default.Book
            "star" -> Icons.Default.Star
            else -> Icons.Default.MoreHoriz
        }
    }

    val availableIcons = listOf(
        "Person", "Work", "School", "Folder", "FitnessCenter", "ShoppingCart", "Home", "Book", "Star", "MoreHoriz"
    )
}

