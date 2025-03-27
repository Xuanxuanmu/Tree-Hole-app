package com.example.myapplication.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import com.example.myapplication.data.model.Post
import com.example.myapplication.ui.viewmodel.PostViewModel
import com.example.myapplication.ui.viewmodel.UserViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "MainScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userViewModel: UserViewModel,
    startDestination: String = "home"
) {
    // 创建共享的PostViewModel实例
    val postViewModel: PostViewModel = viewModel()
    
    val navController = rememberNavController()
    val currentNavBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentNavBackStackEntry?.destination?.route ?: startDestination
    
    // 监听ViewModel中的帖子数据
    val posts by postViewModel.posts.collectAsState()
    
    // 添加Firebase测试按钮的状态
    var showTestButton by remember { mutableStateOf(true) }
    var testResult by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    
    // 在每次MainScreen重组时记录一下状态
    Log.d(TAG, "MainScreen重组: 当前路由=$currentRoute, 帖子数量=${posts.size}")

    Scaffold(
        topBar = {
            if (showTestButton) {
                TopAppBar(
                    title = { Text("树洞") },
                    actions = {
                        // 添加测试按钮
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        Log.d(TAG, "开始Firebase测试...")
                                        
                                        // 直接使用Firestore尝试写入数据
                                        val db = FirebaseFirestore.getInstance()
                                        val testData = hashMapOf(
                                            "content" to "测试帖子 - ${System.currentTimeMillis()}",
                                            "authorName" to "测试用户",
                                            "createdAt" to com.google.firebase.Timestamp.now()
                                        )
                                        
                                        Log.d(TAG, "开始向'test_posts'集合写入数据")
                                        val docRef = db.collection("test_posts").add(testData).await()
                                        
                                        testResult = "测试成功! ID: ${docRef.id}"
                                        Log.d(TAG, testResult)
                                        
                                        // 使用PostViewModel尝试创建帖子
                                        val post = Post(
                                            content = "通过ViewModel创建的测试帖子",
                                            authorName = "测试用户"
                                        )
                                        
                                        Log.d(TAG, "开始通过PostViewModel创建帖子")
                                        postViewModel.createPost(
                                            content = post.content,
                                            authorId = "test_id",
                                            authorName = post.authorName
                                        )
                                        
                                        // 测试完成后刷新帖子列表
                                        delay(1000) // 等待一秒以确保数据已同步
                                        postViewModel.loadPosts()
                                        
                                        showTestButton = false // 测试成功后隐藏按钮
                                    } catch (e: Exception) {
                                        testResult = "测试失败: ${e.message}"
                                        Log.e(TAG, testResult, e)
                                    }
                                }
                            }
                        ) {
                            Text("测试Firebase")
                        }
                        
                        // 显示测试结果
                        if (testResult.isNotEmpty()) {
                            Text(
                                text = testResult,
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = if (testResult.startsWith("测试成功")) Color.Green else Color.Red,
                                fontSize = 12.sp
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (currentRoute != "createPost") {
                NavigationBar {
                    listOf(
                        NavigationItem(
                            title = "主页",
                            selectedIcon = Icons.Filled.Home,
                            unselectedIcon = Icons.Outlined.Home,
                            route = "home"
                        ),
                        NavigationItem(
                            title = "个人",
                            selectedIcon = Icons.Filled.Person,
                            unselectedIcon = Icons.Outlined.Person,
                            route = "profile"
                        )
                    ).forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                // 如果不在当前页面，才导航过去
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        // 避免无限回退栈
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        /*if (item.badgeCount != null) {
                                            Badge {
                                                Text(text = item.badgeCount.toString())
                                            }
                                        }*/
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (currentRoute == item.route) {
                                            item.selectedIcon
                                        } else item.unselectedIcon,
                                        contentDescription = item.title
                                    )
                                }
                            },
                            label = { Text(text = item.title) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == "home") {
                FloatingActionButton(
                    onClick = {
                        navController.navigate("createPost")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "发布新帖子"
                    )
                }
            }
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            userViewModel = userViewModel,
            postViewModel = postViewModel
        )
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel,
    postViewModel: PostViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(postViewModel = postViewModel)
        }
        
        composable("profile") {
            ProfileScreen(userViewModel = userViewModel, postViewModel = postViewModel)
        }

        composable("createPost") {
            CreatePostScreen(
                userViewModel = userViewModel,
                postViewModel = postViewModel,
                onPostCreated = {
                    // 先返回到首页
                    navController.popBackStack()
                    
                    // 返回后再刷新，避免阻塞UI
                    postViewModel.loadPosts()
                    Log.d(TAG, "从发布页面返回，已触发刷新帖子列表")
                }
            )
        }

        /*composable(
            route = "postDetail/{postId}",
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            PostDetailScreen(
                postId = postId,
                postViewModel = postViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }*/
    }
}

data class NavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String,
    val badgeCount: Int? = null
) 