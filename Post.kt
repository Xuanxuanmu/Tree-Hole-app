package com.example.myapplication.data.model

import com.google.firebase.Timestamp

data class Post(
    val id: String = "",
    val content: String = "",
    val authorId: String = "",
    val authorName: String = "匿名用户",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val likes: Int = 0,
    val comments: Int = 0,
    val tags: List<String> = emptyList()
) 