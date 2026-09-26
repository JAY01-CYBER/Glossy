#include <android/log.h>

#define LOG_TAG "GlossyEngine"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#include <jni.h>
#include <string>
#include "AudioDecoder.h"

extern "C" {
#include <libavformat/avformat.h>
}

AudioDecoder* decoder = nullptr;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_jay_glossy_ui_player_NativeEngine_nInitEngine(JNIEnv *env, jobject thiz) {
    if (!decoder) {
        decoder = new AudioDecoder();
    }
    LOGD("Glossy Native Engine FFmpeg + Oboe ke sath Initialize ho gaya! 🚀");
    return JNI_TRUE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_jay_glossy_ui_player_NativeEngine_nPlayUrl(JNIEnv *env, jobject thiz, jstring jUrl) {
    if (!decoder) {
        decoder = new AudioDecoder();
    } else {
        decoder->stop(); 
    }
    
    const char *urlStr = env->GetStringUTFChars(jUrl, nullptr);
    std::string url(urlStr);
    env->ReleaseStringUTFChars(jUrl, urlStr);
    
    return decoder->openUrl(url) ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_jay_glossy_ui_player_NativeEngine_nPause(JNIEnv *env, jobject thiz) {
    if (decoder != nullptr) {
        decoder->pause();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_jay_glossy_ui_player_NativeEngine_nResume(JNIEnv *env, jobject thiz) {
    if (decoder != nullptr) {
        decoder->resume();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_jay_glossy_ui_player_NativeEngine_nStop(JNIEnv *env, jobject thiz) {
    if (decoder != nullptr) {
        decoder->stop();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_jay_glossy_ui_player_NativeEngine_nSeekTo(JNIEnv *env, jobject thiz, jlong position_ms) {
    if (decoder != nullptr) {
        decoder->seekTo(position_ms);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_jay_glossy_ui_player_NativeEngine_nSetVolume(JNIEnv *env, jobject thiz, jfloat vol) {
    if (decoder != nullptr) {
        decoder->setVolume(vol);
    }
}
