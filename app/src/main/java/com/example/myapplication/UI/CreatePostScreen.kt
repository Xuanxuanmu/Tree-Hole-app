package com.example.myapplication.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.TreeholeApplication
import com.example.myapplication.ui.viewmodel.PostViewModel
import com.example.myapplication.ui.viewmodel.UserViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "CreatePostScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    postViewModel: PostViewModel,
    userViewModel: UserViewModel,
    onPostCreated: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var content by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val userProfile by userViewModel.userProfile.collectAsState()

    // 用户信息
    val currentUserId = TreeholeApplication.getCurrentUserId() ?: ""
    // 如果未登录或没有用户资料，则默认为"匿名用户"
    val authorName = if (currentUserId.isEmpty() || userProfile == null) "匿名用户" else userProfile!!.username

    Log.d(TAG, "当前用户: ID=$currentUserId, 名称=$authorName, 是否匿名=${currentUserId.isEmpty()}")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("发布新帖子") },
                navigationIcon = {
                    IconButton(onClick = { onPostCreated() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("有什么想分享的吗？（内容不能为空）") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )
                
                Text(
                    text = "${content.length}/500 字",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (content.length > 500) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    if (content.isBlank()) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("内容不能为空")
                        }
                        return@Button
                    }
                    
                    if (content.length > 500) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("内容不能超过500字")
                        }
                        return@Button
                    }
                    
                    // 创建帖子 - 不等待结果，立即执行
                    Log.d(TAG, "即将发布帖子: 作者=$authorName")
                    postViewModel.createPost(
                        content = content.trim(),
                        authorId = currentUserId,
                        authorName = if (currentUserId.isEmpty()) "匿名用户" else authorName
                    )
                    
                    // 直接返回首页，不需要等待
                    Log.d(TAG, "已提交帖子，立即返回首页")
                    onPostCreated()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("发布")
                }
            }
        }
    }
} 