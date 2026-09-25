#include <jni.h>
#include <string>
#include "audio_decoder.h"

AudioDecoder* decoder = nullptr;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_jay_glossy_player_NativeEngine_nInitEngine(JNIEnv *env, jobject thiz) {
    if (!decoder) {
        decoder = new AudioDecoder();
    }
    
    // FFmpeg ke internet aur network components ko on karna
    avformat_network_init();
    LOGD("Glossy Native Engine FFmpeg ke sath Initialize ho gaya!");
    
    return JNI_TRUE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_jay_glossy_player_NativeEngine_nPlayUrl(JNIEnv *env, jobject thiz, jstring jUrl) {
    if (!decoder) {
        LOGE("Engine initialize nahi hua hai!");
        return JNI_FALSE;
    }
    
    // Kotlin ki String ko C++ string me convert karna
    const char *urlStr = env->GetStringUTFChars(jUrl, nullptr);
    std::string url(urlStr);
    env->ReleaseStringUTFChars(jUrl, urlStr);
    
    return decoder->openUrl(url) ? JNI_TRUE : JNI_FALSE;
}
