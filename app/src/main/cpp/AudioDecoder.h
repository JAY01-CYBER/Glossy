#ifndef GLOSSY_AUDIO_DECODER_H
#define GLOSSY_AUDIO_DECODER_H

#include <string>
#include <android/log.h>

// FFmpeg C libraries ko C++ me include karne ke liye extern "C" zaroori hai
extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libswresample/swresample.h>
#include <libavutil/opt.h>
}

#define LOG_TAG "GlossyDecoder"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

class AudioDecoder {
public:
    AudioDecoder();
    ~AudioDecoder();

    // URL open karke audio stream aur codec dhundhega
    bool openUrl(const std::string& url);
    
    // Audio packets ko read karke raw PCM me decode karega
    int decodeFrame(uint8_t** outBuffer);
    
    // Naye Playback Controls
    void pause();
    void resume();
    void stop();
    
    void release();

    int getSampleRate() const { return targetSampleRate; }
    int getChannelCount() const { return targetChannels; }

private:
    AVFormatContext* formatCtx = nullptr;
    AVCodecContext* codecCtx = nullptr;
    AVFrame* frame = nullptr;
    AVPacket* packet = nullptr;
    
    SwrContext* swrCtx = nullptr; 

    int audioStreamIndex = -1;
    
    int targetSampleRate = 48000;
    int targetChannels = 2; 

    // Playback state track karne ke liye
    bool isPlaying = false;
    bool isPaused = false;
};

#endif // GLOSSY_AUDIO_DECODER_H
