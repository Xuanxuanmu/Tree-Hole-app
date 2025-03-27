package com.example.myapplication

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class TreeholeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 初始化Firebase
        FirebaseApp.initializeApp(this)
        
        // 确保有一个匿名用户
        val auth = Firebase.auth
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnSuccessListener {
                    println("匿名用户登录成功: ${it.user?.uid}")
                }
                .addOnFailureListener { e ->
                    println("匿名用户登录失败: ${e.message}")
                }
        }
    }
    
    companion object {
        private lateinit var instance: TreeholeApplication
        private const val PREF_NAME = "treehole_prefs"
        private const val KEY_ANONYMOUS_POSTS = "anonymous_posts"
        
        // 获取当前用户ID，如果未登录返回null
        fun getCurrentUserId(): String? {
            return FirebaseAuth.getInstance().currentUser?.uid
        }
        
        // 保存匿名帖子ID
        fun saveAnonymousPostId(postId: String) {
            if (postId.isEmpty()) return
            
            val prefs = instance.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val existingPostIds = getAnonymousPostIds().toMutableSet()
            existingPostIds.add(postId)
            
            prefs.edit().putStringSet(KEY_ANONYMOUS_POSTS, existingPostIds).apply()
        }
        
        // 获取所有匿名帖子ID
        fun getAnonymousPostIds(): Set<String> {
            val prefs = instance.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getStringSet(KEY_ANONYMOUS_POSTS, emptySet()) ?: emptySet()
        }
        
        // 检查帖子是否是当前设备上的匿名用户发布的
        fun isAnonymousPostByCurrentDevice(postId: String): Boolean {
            return getAnonymousPostIds().contains(postId)
        }
    }
}