package com.example.myapplication.data.model

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    var username: String = "",
    var email: String = "",
    var avatarUrl: String = "",
    var bio: String = "",
    var createdAt: Timestamp = Timestamp.now(),
    var emailVerified: Boolean = false
) 