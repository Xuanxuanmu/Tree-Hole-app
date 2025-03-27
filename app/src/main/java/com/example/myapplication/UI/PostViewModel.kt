package com.example.myapplication.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.TreeholeApplication
import com.example.myapplication.data.model.Post
import com.example.myapplication.data.repository.PostRepository
import com.example.myapplication.data.repository.CommentRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PostViewModel : ViewModel() {
    private val TAG = "PostViewModel"
    private val postRepository = PostRepository()
    private val commentRepository = CommentRepository()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _userPosts = MutableStateFlow<List<Post>>(emptyList())
    val userPosts: StateFlow<List<Post>> = _userPosts.asStateFlow()

    private val _anonymousPosts = MutableStateFlow<List<Post>>(emptyList())
    val anonymousPosts: StateFlow<List<Post>> = _anonymousPosts.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadPosts()
        // 初始化时加载匿名帖子
        loadAnonymousPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                postRepository.getPosts().collect { posts ->
                    Log.d(TAG, "获取到 ${posts.size} 条帖子")
                    _posts.value = posts
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载帖子失败: ${e.message}", e)
                _isLoading.value = false
            }
        }
    }

    fun searchPosts(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _searchQuery.value = query
            try {
                postRepository.searchPosts(query).collect { posts ->
                    _posts.value = posts
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "搜索帖子失败: ${e.message}", e)
                _isLoading.value = false
            }
        }
    }

    fun getUserPosts(userId: String = TreeholeApplication.getCurrentUserId() ?: "") {
        if (userId.isEmpty()) {
            Log.e(TAG, "无法获取用户帖子: 用户ID为空")
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                postRepository.getUserPosts(userId).collect { posts ->
                    _userPosts.value = posts
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取用户帖子失败: ${e.message}", e)
                _isLoading.value = false
            }
        }
    }

    fun createPost(content: String, authorId: String = TreeholeApplication.getCurrentUserId() ?: "", authorName: String) {
        // 如果authorId为空，就用"anonymous"作为默认值
        val finalAuthorId = if (authorId.isEmpty()) "anonymous" else authorId
        
        viewModelScope.launch {
            try {
                val post = Post(
                    content = content,
                    authorId = finalAuthorId,
                    authorName = authorName
                )
                
                // 执行创建帖子操作
                val result = postRepository.createPost(post)
                
                if (result.isSuccess) {
                    val postId = result.getOrNull()
                    Log.d(TAG, "帖子创建成功: $postId")
                    
                    // 如果是匿名帖子，保存到本地
                    if (authorId.isEmpty() && postId != null) {
                        TreeholeApplication.saveAnonymousPostId(postId)
                    }
                } else {
                    Log.e(TAG, "帖子创建失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "创建帖子时发生错误: ${e.message}", e)
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                val result = postRepository.deletePost(postId)
                if (result.isSuccess) {
                    Log.d(TAG, "帖子删除成功: $postId")
                    
                    // 更新列表
                    val currentPosts = _posts.value.toMutableList()
                    val currentUserPosts = _userPosts.value.toMutableList()
                    
                    _posts.value = currentPosts.filter { it.id != postId }
                    _userPosts.value = currentUserPosts.filter { it.id != postId }
                } else {
                    Log.e(TAG, "帖子删除失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "删除帖子时发生错误: ${e.message}", e)
            }
        }
    }

    fun updatePost(postId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            try {
                val result = postRepository.updatePost(postId, updates)
                if (result.isSuccess) {
                    Log.d(TAG, "帖子更新成功: $postId")
                    // 更新成功后刷新列表
                    loadPosts()
                    val currentUserId = TreeholeApplication.getCurrentUserId()
                    if (currentUserId != null) {
                        getUserPosts(currentUserId)
                    }
                } else {
                    Log.e(TAG, "帖子更新失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "更新帖子时发生错误: ${e.message}", e)
            }
        }
    }

    // 加载匿名用户的帖子
    fun loadAnonymousPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val anonymousPostIds = TreeholeApplication.getAnonymousPostIds()
                Log.d(TAG, "获取到 ${anonymousPostIds.size} 个匿名帖子ID")
                
                if (anonymousPostIds.isEmpty()) {
                    _anonymousPosts.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }
                
                // 从所有帖子中筛选出匿名帖子
                postRepository.getPosts().collect { allPosts ->
                    val myAnonymousPosts = allPosts.filter { post -> 
                        anonymousPostIds.contains(post.id) 
                    }
                    Log.d(TAG, "筛选出 ${myAnonymousPosts.size} 条匿名帖子")
                    _anonymousPosts.value = myAnonymousPosts
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载匿名帖子失败: ${e.message}", e)
                _isLoading.value = false
            }
        }
    }
} 