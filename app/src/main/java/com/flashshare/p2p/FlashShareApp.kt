package com.flashshare.p2p

import android.app.Application
import android.util.Log

/**
 * FlashShareApp - Application entry point.
 * Initializes core logging and global configuration for zero-loss P2P transfers.
 */
class FlashShareApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "FlashShare P2P Engine Initialized - Ready for ultra-fast transfers")
    }

    companion object {
        private const val TAG = "FlashShareApp"
    }
}
