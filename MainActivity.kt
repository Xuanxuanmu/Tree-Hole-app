package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.screens.LoginScreen
import com.example.myapplication.ui.screens.MainScreen
import com.example.myapplication.ui.screens.RegisterScreen
import com.example.myapplication.ui.theme.TreeholeTheme
import com.example.myapplication.ui.viewmodel.UserViewModel

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "应用启动")
        
        setContent {
            // 创建UserViewModel实例
            val userViewModel: UserViewModel = viewModel()
            val isLoggedIn by userViewModel.isLoggedIn.collectAsState()
            
            TreeholeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 顶层导航
                    val navController = rememberNavController()
                    var startDestination by remember { mutableStateOf("login") } 
                    
                    // 如果用户已登录，跳转到主界面
                    LaunchedEffect(isLoggedIn) {
                        if (isLoggedIn) {
                            startDestination = "main"
                        }
                    }
                    
                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        // 登录界面
                        composable("login") {
                            LoginScreen(
                                userViewModel = userViewModel,
                                onLoginSuccess = {
                                    navController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                }
                            )
                        }
                        
                        // 注册界面
                        composable("register") {
                            RegisterScreen(
                                userViewModel = userViewModel,
                                onRegisterSuccess = {
                                    navController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToLogin = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        // 主界面
                        composable("main") {
                            MainScreen(userViewModel = userViewModel)
                        }
                    }
                }
            }
        }
    }
}