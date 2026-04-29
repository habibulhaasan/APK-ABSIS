package com.absis.capitalsync.core.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.storage.FirebaseStorage

object FirebaseModule {

    // Auth
    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // Firestore with OFFLINE persistence enabled
    val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = firestoreSettings {
                // Enables offline caching (like your offline functionality goal)
                isPersistenceEnabled = true
                cacheSizeBytes = FirebaseFirestore.CACHE_SIZE_UNLIMITED
            }
        }
    }

    // Storage
    val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }
}