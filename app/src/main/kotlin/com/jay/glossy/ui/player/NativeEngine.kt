package com.jay.glossy.ui.player

class NativeEngine {
    
    companion object {
        // C++ library load karna
        init {
            System.loadLibrary("glossy_engine")
        }
    }

    // C++ ke JNI Functions
    external fun nInitEngine(): Boolean
    external fun nPlayUrl(url: String): Boolean
    
    // Playback controls
    external fun nPause()
    external fun nResume()
    external fun nStop()
    
    // Seek aur Volume controls
    external fun nSeekTo(positionMs: Long)
    external fun nSetVolume(volume: Float)
}
