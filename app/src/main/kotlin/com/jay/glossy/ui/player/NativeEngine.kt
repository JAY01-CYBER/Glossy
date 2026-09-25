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
    
    // Naye Pause, Resume, Stop controls
    external fun nPause()
    external fun nResume()
    external fun nStop()
}
