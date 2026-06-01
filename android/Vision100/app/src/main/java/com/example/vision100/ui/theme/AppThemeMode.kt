package com.example.vision100.ui.theme

enum class AppThemeMode {
    System,
    Light,
    Dark;

    companion object {
        fun fromStoredValue(value: String?): AppThemeMode {
            return entries.firstOrNull { it.name == value } ?: System
        }
    }
}
