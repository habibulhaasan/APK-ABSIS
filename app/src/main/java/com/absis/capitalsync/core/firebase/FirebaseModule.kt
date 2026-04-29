package com.absis.capitalsync.core.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.storage.FirebaseStorage

object FirebaseModule {

    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = firestoreSettings {
                setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder().build()
                )
            }
        }
    }

    val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }
}