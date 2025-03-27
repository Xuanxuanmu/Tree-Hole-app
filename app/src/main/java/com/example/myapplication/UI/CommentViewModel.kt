package com.example.myapplication.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.TreeholeApplication
import com.example.myapplication.data.model.Comment
import com.example.myapplication.data.repository.CommentRepository
import com.example.myapplication.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommentViewModel : ViewModel() {
    private val TAG = "CommentViewModel"
    private val commentRepository = CommentRepository()
    private val postRepository = PostRepository()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadComments(postId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(TAG, "加载帖子 $postId 的评论")
                commentRepository.getCommentsForPost(postId).collect { commentsList ->
                    _comments.value = commentsList
                    _isLoading.value = false
                    Log.d(TAG, "成功加载 ${commentsList.size} 条评论")
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载评论失败: ${e.message}", e)
                _isLoading.value = false
            }
        }
    }

    suspend fun addComment(postId: String, content: String, authorName: String = "匿名用户"): Result<String> {
        if (content.isBlank()) {
            return Result.failure(IllegalArgumentException("评论内容不能为空"))
        }

        val authorId = TreeholeApplication.getCurrentUserId() ?: ""
        
        return try {
            Log.d(TAG, "添加评论: postId=$postId, authorId=$authorId, content=$content")
            
            val comment = Comment(
                postId = postId,
                content = content,
                authorId = authorId,
                authorName = authorName
            )
            
            val result = commentRepository.addComment(comment)
            
            if (result.isSuccess) {
                // 更新帖子的评论计数
                val commentId = result.getOrNull()
                Log.d(TAG, "评论添加成功: $commentId")
                
                try {
                    // 更新帖子的评论数
                    postRepository.updatePost(postId, mapOf("comments" to _comments.value.size + 1))
                    Log.d(TAG, "更新帖子评论计数成功")
                } catch (e: Exception) {
                    Log.e(TAG, "更新帖子评论计数失败: ${e.message}", e)
                }
                
                // 重新加载评论
                loadComments(postId)
            } else {
                Log.e(TAG, "评论添加失败: ${result.exceptionOrNull()?.message}")
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "添加评论时发生错误: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun deleteComment(commentId: String, postId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "删除评论: $commentId")
                val result = commentRepository.deleteComment(commentId)
                
                if (result.isSuccess) {
                    Log.d(TAG, "评论删除成功")
                    
                    // 更新列表
                    _comments.value = _comments.value.filter { it.id != commentId }
                    
                    // 更新帖子的评论计数
                    try {
                        postRepository.updatePost(postId, mapOf("comments" to _comments.value.size))
                        Log.d(TAG, "更新帖子评论计数成功")
                    } catch (e: Exception) {
                        Log.e(TAG, "更新帖子评论计数失败: ${e.message}", e)
                    }
                } else {
                    Log.e(TAG, "评论删除失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "删除评论时发生错误: ${e.message}", e)
            }
        }
    }
} 