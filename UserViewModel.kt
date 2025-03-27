package com.example.myapplication.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.TreeholeApplication
import com.example.myapplication.data.model.User
import com.example.myapplication.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {
    private val TAG = "UserViewModel"
    private val userRepository = UserRepository()

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        // 检查用户是否已登录
        val user = userRepository.getCurrentUser()
        _isLoggedIn.value = user != null
        _currentUser.value = user
        
        // 如果用户登录了，获取用户资料
        val userId = TreeholeApplication.getCurrentUserId()
        if (userId != null) {
            loadUserProfile(userId)
        }
    }

    // 邮箱注册
    suspend fun registerWithEmail(email: String, password: String, username: String): Result<String> {
        _isLoading.value = true
        return try {
            val result = userRepository.registerWithEmail(email, password, username)
            if (result.isSuccess) {
                val userId = result.getOrNull()
                Log.d(TAG, "用户注册成功: $userId")
                
                // 更新登录状态
                _isLoggedIn.value = true
                _currentUser.value = userRepository.getCurrentUser()
                
                // 获取用户资料
                if (userId != null) {
                    loadUserProfile(userId)
                }
            } else {
                Log.e(TAG, "用户注册失败: ${result.exceptionOrNull()?.message}")
            }
            _isLoading.value = false
            result
        } catch (e: Exception) {
            Log.e(TAG, "用户注册异常: ${e.message}", e)
            _isLoading.value = false
            Result.failure(e)
        }
    }
    
    // 邮箱登录
    suspend fun loginWithEmail(email: String, password: String): Result<String> {
        _isLoading.value = true
        return try {
            val result = userRepository.loginWithEmail(email, password)
            if (result.isSuccess) {
                val userId = result.getOrNull()
                Log.d(TAG, "用户登录成功: $userId")
                
                // 更新登录状态
                _isLoggedIn.value = true
                _currentUser.value = userRepository.getCurrentUser()
                
                // 获取用户资料
                if (userId != null) {
                    loadUserProfile(userId)
                }
            } else {
                Log.e(TAG, "用户登录失败: ${result.exceptionOrNull()?.message}")
            }
            _isLoading.value = false
            result
        } catch (e: Exception) {
            Log.e(TAG, "用户登录异常: ${e.message}", e)
            _isLoading.value = false
            Result.failure(e)
        }
    }
    
    // 邮箱验证
    suspend fun sendEmailVerification(): Result<Unit> {
        return userRepository.sendEmailVerification()
    }
    
    // 注销
    fun logout() {
        userRepository.logout()
        _isLoggedIn.value = false
        _currentUser.value = null
        _userProfile.value = null
    }

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = userRepository.getUserProfile(userId)
                if (result.isSuccess) {
                    val profile = result.getOrNull()
                    if (profile != null) {
                        Log.d(TAG, "用户资料加载成功: ${profile.username}")
                        _userProfile.value = profile
                    } else {
                        Log.d(TAG, "用户资料为空")
                    }
                } else {
                    Log.e(TAG, "用户资料加载失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "用户资料加载异常: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUserProfile(updates: Map<String, Any>) {
        val userId = TreeholeApplication.getCurrentUserId() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = userRepository.updateUserProfile(userId, updates)
                if (result.isSuccess) {
                    Log.d(TAG, "用户资料更新成功")
                    loadUserProfile(userId) // 重新加载用户资料
                } else {
                    Log.e(TAG, "用户资料更新失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "用户资料更新异常: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
} 