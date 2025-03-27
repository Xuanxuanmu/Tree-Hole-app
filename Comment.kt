package com.example.myapplication.data.model

import com.google.firebase.Timestamp

data class Comment(
    val id: String = "",
    val postId: String = "",
    val content: String = "",
    val authorId: String = "",
    val authorName: String = "匿名用户",
    val createdAt: Timestamp = Timestamp.now(),
    val likes: Int = 0
) 