package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.model.Comment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class CommentRepository {
    private val TAG = "CommentRepository"
    private val firestore = FirebaseFirestore.getInstance()
    private val commentsCollection = firestore.collection("comments")

    suspend fun addComment(comment: Comment): Result<String> = try {
        Log.d(TAG, "添加评论: $comment")
        val docRef = commentsCollection.add(comment).await()
        val commentId = docRef.id
        
        // 更新评论ID
        commentsCollection.document(commentId).update("id", commentId).await()
        
        Log.d(TAG, "评论添加成功，ID: $commentId")
        Result.success(commentId)
    } catch (e: Exception) {
        Log.e(TAG, "添加评论失败: ${e.message}", e)
        Result.failure(e)
    }

    fun getCommentsForPost(postId: String): Flow<List<Comment>> = flow {
        try {
            Log.d(TAG, "获取帖子评论: $postId")
            val snapshot = commentsCollection
                .whereEqualTo("postId", postId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val comments = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Comment::class.java)?.copy(id = doc.id)
            }
            Log.d(TAG, "获取到 ${comments.size} 条评论")
            emit(comments)
        } catch (e: Exception) {
            Log.e(TAG, "获取评论失败: ${e.message}", e)
            emit(emptyList())
        }
    }

    suspend fun deleteComment(commentId: String): Result<Unit> = try {
        Log.d(TAG, "删除评论: $commentId")
        commentsCollection.document(commentId).delete().await()
        Log.d(TAG, "评论删除成功")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "删除评论失败: ${e.message}", e)
        Result.failure(e)
    }
} 