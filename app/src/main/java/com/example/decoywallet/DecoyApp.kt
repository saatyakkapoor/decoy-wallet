package com.example.decoywallet

import android.app.Application
import com.example.decoywallet.model.WalletStore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * App entry point. Initialises the persisted wallet store and signs the device
 * in anonymously so it can upload intruder evidence to Firebase.
 */
class DecoyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        WalletStore.init(this)

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }
    }

    companion object {
        /**
         * Lives beyond any single Activity so intruder capture + upload can
         * finish after the login screen has handed off to the decoy wallet.
         */
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
}
