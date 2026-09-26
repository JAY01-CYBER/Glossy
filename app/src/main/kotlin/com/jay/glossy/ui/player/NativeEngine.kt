package com.jay.glossy.ui.player

class NativeEngine {
    
    companion object {
        init {
            System.loadLibrary("glossy_engine")
        }
    }

    external fun nInitEngine(): Boolean
    external fun nPlayUrl(url: String): Boolean
    
    external fun nPause()
    external fun nResume()
    external fun nStop()
    
    external fun nSeekTo(positionMs: Long)
    external fun nSetVolume(volume: Float)
    
    // YE NAYA FUNCTION CURRENT TIME LANE KE LIYE HAI
    external fun nGetCurrentPosition(): Long
}
