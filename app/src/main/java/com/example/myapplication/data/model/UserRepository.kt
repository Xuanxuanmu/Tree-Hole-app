package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val TAG = "UserRepository"
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    // 邮箱注册
    suspend fun registerWithEmail(email: String, password: String, username: String): Result<String> = try {
        Log.d(TAG, "开始邮箱注册: $email, 用户名: $username")
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val userId = authResult.user?.uid
        
        if (userId != null) {
            // 设置用户资料
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .build()
            
            auth.currentUser?.updateProfile(profileUpdates)?.await()
            
            // 创建用户文档
            val user = User(
                id = userId,
                email = email,
                username = username,
                createdAt = com.google.firebase.Timestamp.now()
            )
            
            usersCollection.document(userId).set(user).await()
            Log.d(TAG, "用户注册成功: $userId")
            Result.success(userId)
        } else {
            Log.e(TAG, "用户注册失败: 无法获取用户ID")
            Result.failure(Exception("无法获取用户ID"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "用户注册失败: ${e.message}", e)
        Result.failure(e)
    }
    
    // 邮箱登录
    suspend fun loginWithEmail(email: String, password: String): Result<String> = try {
        Log.d(TAG, "开始邮箱登录: $email")
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        val userId = authResult.user?.uid
        
        if (userId != null) {
            Log.d(TAG, "用户登录成功: $userId")
            Result.success(userId)
        } else {
            Log.e(TAG, "用户登录失败: 无法获取用户ID")
            Result.failure(Exception("无法获取用户ID"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "用户登录失败: ${e.message}", e)
        Result.failure(e)
    }
    
    // 邮箱验证
    suspend fun sendEmailVerification(): Result<Unit> = try {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "发送邮箱验证: ${currentUser.email}")
            currentUser.sendEmailVerification().await()
            Log.d(TAG, "邮箱验证发送成功")
            Result.success(Unit)
        } else {
            Log.e(TAG, "发送邮箱验证失败: 用户未登录")
            Result.failure(Exception("用户未登录"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "发送邮箱验证失败: ${e.message}", e)
        Result.failure(e)
    }
    
    // 注销
    fun logout() {
        auth.signOut()
        Log.d(TAG, "用户已注销")
    }
    
    // 获取当前用户
    fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser
        return if (firebaseUser != null) {
            User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                username = firebaseUser.displayName ?: "用户${firebaseUser.uid.take(5)}",
                emailVerified = firebaseUser.isEmailVerified
            )
        } else {
            null
        }
    }
    
    // 检查用户是否已登录
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // 以下是原有的用户资料相关方法
    suspend fun getUserProfile(userId: String): Result<User?> = try {
        Log.d(TAG, "获取用户资料: $userId")
        val doc = usersCollection.document(userId).get().await()
        val user = doc.toObject(User::class.java)
        
        // 如果用户存在，返回用户资料
        if (user != null) {
            Log.d(TAG, "获取用户资料成功: $user")
            Result.success(user)
        } else {
            // 如果用户不存在，可能是匿名用户，创建一个默认用户资料
            val defaultUser = User(
                id = userId,
                username = "用户${userId.take(5)}",
                email = "",
                createdAt = com.google.firebase.Timestamp.now()
            )
            Log.d(TAG, "用户不存在，创建默认资料: $defaultUser")
            Result.success(defaultUser)
        }
    } catch (e: Exception) {
        Log.e(TAG, "获取用户资料失败: ${e.message}", e)
        Result.failure(e)
    }

    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Result<Unit> = try {
        Log.d(TAG, "更新用户资料: $userId, 更新: $updates")
        usersCollection.document(userId).update(updates).await()
        Log.d(TAG, "用户资料更新成功")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "更新用户资料失败: ${e.message}", e)
        Result.failure(e)
    }

    suspend fun createUserProfile(user: User): Result<Unit> {
        return try {
            Log.d(TAG, "创建用户资料: ${user.id}")
            usersCollection.document(user.id).set(user).await()
            Log.d(TAG, "用户资料创建成功")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "创建用户资料失败: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteUserProfile(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "删除用户资料: $userId")
            usersCollection.document(userId).delete().await()
            Log.d(TAG, "用户资料删除成功")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "删除用户资料失败: ${e.message}", e)
            Result.failure(e)
        }
    }
} 