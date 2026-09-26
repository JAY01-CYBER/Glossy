#ifndef GLOSSY_AUDIO_DECODER_H
#define GLOSSY_AUDIO_DECODER_H

#include <string>
#include <memory>
#include <android/log.h>
#include <oboe/Oboe.h>

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libswresample/swresample.h>
#include <libavutil/opt.h>
}

#define LOG_TAG "GlossyDecoder"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// Oboe Data Callback inherit karna zaroori hai
class AudioDecoder : public oboe::AudioStreamDataCallback {
public:
    AudioDecoder();
    ~AudioDecoder();

    bool openUrl(const std::string& url);
    
    void pause();
    void resume();
    void stop();
    void release();

    // Ye function Oboe hardware call karega jab usko naya audio data chahiye hoga
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;

private:
    AVFormatContext* formatCtx = nullptr;
    AVCodecContext* codecCtx = nullptr;
    AVFrame* frame = nullptr;
    AVPacket* packet = nullptr;
    SwrContext* swrCtx = nullptr; 

    // Oboe Audio Stream
    std::shared_ptr<oboe::AudioStream> audioStream;

    int audioStreamIndex = -1;
    int targetSampleRate = 48000;
    int targetChannels = 2; 

    bool isPlaying = false;
    bool isPaused = false;

    // Buffer for holding decoded PCM data
    uint8_t* outBuffer = nullptr;
    int outBufferSize = 0;
    int outBufferIndex = 0;

    // Internal decode function
    int decodeNextFrame();
};

#endif // GLOSSY_AUDIO_DECODER_H
