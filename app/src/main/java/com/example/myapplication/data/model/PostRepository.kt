package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class PostRepository {
    private val TAG = "PostRepository"
    private val firestore = FirebaseFirestore.getInstance()
    private val postsCollection = firestore.collection("posts")

    suspend fun createPost(post: Post): Result<String> = try {
        // 更明确的日志，便于调试
        Log.d(TAG, "正在创建帖子: 集合路径=${postsCollection.path}, 帖子内容=${post.content.take(20)}...")
        
        // 移除id字段，让Firestore自动生成
        val newPost = post.copy(id = "")
        
        // 确保createdAt是一个有效的Timestamp
        val postWithTimestamp = if (newPost.createdAt == null) {
            newPost.copy(createdAt = com.google.firebase.Timestamp.now())
        } else {
            newPost
        }
        
        // 添加日志显示具体的Post对象内容
        Log.d(TAG, "准备添加到Firestore的帖子: $postWithTimestamp")
        
        // 尝试添加文档
        val docRef = postsCollection.add(postWithTimestamp).await()
        val postId = docRef.id
        
        Log.d(TAG, "文档已添加，ID: $postId, 路径: ${docRef.path}")
        
        // 更新文档，添加ID字段
        postsCollection.document(postId).update("id", postId).await()
        
        Log.d(TAG, "帖子创建成功，更新了ID字段")
        
        // 为确认，尝试读取刚创建的文档
        val newDoc = postsCollection.document(postId).get().await()
        if (newDoc.exists()) {
            Log.d(TAG, "确认：文档存在, 数据: ${newDoc.data}")
        } else {
            Log.w(TAG, "警告：无法确认文档是否存在")
        }
        
        Result.success(postId)
    } catch (e: Exception) {
        Log.e(TAG, "创建帖子失败: ${e.message}", e)
        e.printStackTrace() // 打印完整堆栈跟踪
        Result.failure(e)
    }

    fun getPosts(limit: Long = 20): Flow<List<Post>> = flow {
        try {
            Log.d(TAG, "正在获取帖子列表，集合路径=${postsCollection.path}, 限制 $limit 条")
            val snapshot = postsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            
            Log.d(TAG, "Firestore查询完成：获取到 ${snapshot.documents.size} 个文档")
            
            val posts = snapshot.documents.mapNotNull { doc ->
                try {
                    val post = doc.toObject(Post::class.java)?.copy(id = doc.id)
                    if (post != null) {
                        Log.d(TAG, "解析帖子: ID=${post.id}, 作者=${post.authorName}, 内容=${post.content.take(20)}...")
                    } else {
                        Log.e(TAG, "无法解析帖子: ${doc.id}, 数据=${doc.data}")
                    }
                    post
                } catch (e: Exception) {
                    Log.e(TAG, "解析帖子异常: ${doc.id}, 错误=${e.message}", e)
                    null
                }
            }
            
            if (posts.isEmpty()) {
                Log.w(TAG, "警告：获取到0条帖子，尝试检查Firestore控制台")
                // 尝试获取根文档以验证连接
                try {
                    val db = FirebaseFirestore.getInstance()
                    // 不使用listCollections()，而是尝试获取默认集合中的一个文档来验证连接
                    Log.d(TAG, "尝试获取test_posts集合的文档...")
                    val testCollection = db.collection("test_posts").limit(1).get().await()
                    
                    if (testCollection.isEmpty) {
                        Log.d(TAG, "test_posts集合为空或不存在")
                    } else {
                        Log.d(TAG, "成功获取test_posts集合，包含 ${testCollection.size()} 个文档")
                    }
                    
                    // 再检查posts集合
                    val postsCheck = db.collection("posts").limit(1).get().await()
                    if (postsCheck.isEmpty) {
                        Log.d(TAG, "posts集合为空或不存在")
                    } else {
                        Log.d(TAG, "成功获取posts集合，包含 ${postsCheck.size()} 个文档")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "检查集合失败: ${e.message}", e)
                }
            } else {
                Log.d(TAG, "成功获取 ${posts.size} 条帖子")
            }
            
            emit(posts)
        } catch (e: Exception) {
            Log.e(TAG, "获取帖子失败: ${e.message}", e)
            e.printStackTrace() // 打印完整堆栈
            emit(emptyList())
        }
    }

    fun searchPosts(query: String): Flow<List<Post>> = flow {
        try {
            Log.d(TAG, "正在搜索帖子，关键词: $query")
            val snapshot = postsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val posts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(id = doc.id)
            }.filter { post ->
                post.content.contains(query, ignoreCase = true) ||
                post.tags.any { it.contains(query, ignoreCase = true) }
            }
            Log.d(TAG, "搜索到 ${posts.size} 条帖子")
            emit(posts)
        } catch (e: Exception) {
            Log.e(TAG, "搜索帖子失败: ${e.message}", e)
            emit(emptyList())
        }
    }

    fun getUserPosts(userId: String): Flow<List<Post>> = flow {
        try {
            Log.d(TAG, "正在获取用户 $userId 的帖子")
            val snapshot = postsCollection
                .whereEqualTo("authorId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val posts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(id = doc.id)
            }
            Log.d(TAG, "获取到用户 ${posts.size} 条帖子")
            emit(posts)
        } catch (e: Exception) {
            Log.e(TAG, "获取用户帖子失败: ${e.message}", e)
            emit(emptyList())
        }
    }

    suspend fun updatePost(postId: String, updates: Map<String, Any>): Result<Unit> = try {
        Log.d(TAG, "正在更新帖子 $postId")
        postsCollection.document(postId).update(updates).await()
        Log.d(TAG, "帖子更新成功")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "更新帖子失败: ${e.message}", e)
        Result.failure(e)
    }

    suspend fun deletePost(postId: String): Result<Unit> = try {
        Log.d(TAG, "正在删除帖子 $postId")
        postsCollection.document(postId).delete().await()
        Log.d(TAG, "帖子删除成功")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "删除帖子失败: ${e.message}", e)
        Result.failure(e)
    }
} 