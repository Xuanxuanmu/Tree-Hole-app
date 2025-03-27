package com.example.myapplication.ui.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return formatter.format(date)
    }
} 