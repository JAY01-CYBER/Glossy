package com.jay.glossy.ui.player

class NativeEngine {
    
    companion object {
        // C++ library load karna (CMakeLists me glossy_engine naam se banayi hai)
        init {
            System.loadLibrary("glossy_engine")
        }
    }

    // C++ ke JNI Functions
    external fun nInitEngine(): Boolean
    external fun nPlayUrl(url: String): Boolean
}
