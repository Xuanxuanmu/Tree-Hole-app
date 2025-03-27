package com.example.myapplication.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.TreeholeApplication
import com.example.myapplication.data.model.Post
import com.example.myapplication.ui.utils.DateUtils
import com.example.myapplication.ui.viewmodel.PostViewModel
import com.example.myapplication.ui.viewmodel.UserViewModel
import kotlinx.coroutines.launch

private const val TAG = "ProfileScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    postViewModel: PostViewModel,
    userViewModel: UserViewModel
) {
    val userPosts by postViewModel.userPosts.collectAsState()
    val anonymousPosts by postViewModel.anonymousPosts.collectAsState()
    val isLoading by postViewModel.isLoading.collectAsState()
    val userProfile by userViewModel.userProfile.collectAsState()
    val userId = TreeholeApplication.getCurrentUserId() ?: ""
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // 添加强制刷新的状态
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // 添加标签页状态
    val tabOptions = listOf("已登录帖子", "匿名帖子")
    var selectedTabIndex by remember { mutableStateOf(0) }

    // 计算当前要显示的帖子列表
    val currentPosts = when (selectedTabIndex) {
        0 -> userPosts
        1 -> anonymousPosts
        else -> userPosts
    }

    // 在组件首次加载时或刷新时获取数据
    LaunchedEffect(refreshTrigger) {
        // 加载已登录用户的帖子
        if (userId.isNotEmpty()) {
            Log.d(TAG, "获取用户 $userId 的帖子，触发器: $refreshTrigger")
            postViewModel.getUserPosts(userId)
        }
        
        // 加载匿名帖子
        Log.d(TAG, "加载匿名帖子，触发器: $refreshTrigger")
        postViewModel.loadAnonymousPosts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "我的树洞", 
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "刷新",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 用户信息卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = userProfile?.username ?: "匿名用户",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "ID: ${userId.take(8)}...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = if (selectedTabIndex == 0) 
                                "已发布 ${userPosts.size} 条登录帖子" 
                               else 
                                "已发布 ${anonymousPosts.size} 条匿名帖子",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 添加标签页选择器
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabOptions.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            // 内容区域
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (currentPosts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "你还没有发布过帖子",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(onClick = { refreshTrigger++ }) {
                        Text("刷新")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(currentPosts, key = { it.id }) { post ->
                        PostItem(
                            post = post, 
                            onDeleteClick = {
                                coroutineScope.launch {
                                    try {
                                        postViewModel.deletePost(post.id)
                                        snackbarHostState.showSnackbar("帖子已删除")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("删除失败: ${e.message}")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostItem(post: Post, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = post.content,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateUtils.formatDate(post.createdAt.toDate()),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                TextButton(onClick = onDeleteClick) {
                    Text(
                        text = "删除",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
} 