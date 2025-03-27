package com.example.myapplication.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.Post
import com.example.myapplication.ui.utils.DateUtils
import com.example.myapplication.ui.viewmodel.PostViewModel
import kotlinx.coroutines.delay

private const val TAG = "HomeScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(postViewModel: PostViewModel) {
    val posts by postViewModel.posts.collectAsState()
    val isLoading by postViewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // 添加强制刷新的状态
    var refreshTrigger by remember { mutableStateOf(0) }
    // 添加上次刷新时间，确保定期刷新
    var lastRefreshTime by remember { mutableStateOf(0L) }

    // 组件首次加载或刷新时获取帖子
    LaunchedEffect(refreshTrigger) {
        Log.d(TAG, "加载帖子，触发器: $refreshTrigger")
        postViewModel.loadPosts()
        lastRefreshTime = System.currentTimeMillis()
    }

    // 每隔30秒自动刷新一次
    LaunchedEffect(Unit) {
        while(true) {
            delay(30000) // 30秒
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRefreshTime >= 30000) {
                Log.d(TAG, "自动刷新帖子列表")
                postViewModel.loadPosts()
                lastRefreshTime = currentTime
            }
        }
    }

    // 监听帖子列表变化
    LaunchedEffect(posts) {
        Log.d(TAG, "帖子列表已更新，当前有 ${posts.size} 条帖子")
    }

    // 如果是空列表，自动重试几次
    LaunchedEffect(posts.isEmpty()) {
        if (posts.isEmpty() && !isLoading) {
            for (i in 1..3) {
                Log.d(TAG, "帖子列表为空，第 $i 次重试")
                delay(1000)
                postViewModel.loadPosts()
                if (posts.isNotEmpty()) break
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "在树洞里，写下秘密",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (posts.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "还没有帖子，快来发布第一个帖子吧！",
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { refreshTrigger++ }) {
                        Text("刷新")
                    }
                }
            } else {
                LazyColumn {
                    items(posts, key = { it.id }) { post ->
                        PostCard(post = post)
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(post: Post) {
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
            // 帖子内容 - 增加日志
            Log.d("PostCard", "显示帖子: ${post.id}, 内容: ${post.content}")
            Text(
                text = post.content,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 作者和时间
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = post.authorName,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Text(
                    text = DateUtils.formatDate(post.createdAt.toDate()),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // 评论数
            Text(
                text = "评论: ${post.comments}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
} 