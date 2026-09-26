#ifndef GLOSSY_AUDIO_DECODER_H
#define GLOSSY_AUDIO_DECODER_H

#include <string>
#include <memory>
#include <android/log.h>
#include <oboe/Oboe.h>
#include <thread>
#include <mutex>
#include <atomic>
#include <deque>

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libswresample/swresample.h>
#include <libavutil/opt.h>
}

#define LOG_TAG "GlossyDecoder"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

class AudioDecoder : public oboe::AudioStreamDataCallback {
public:
    AudioDecoder();
    ~AudioDecoder();

    bool openUrl(const std::string& url);
    
    void pause();
    void resume();
    void stop();
    void release();

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;

private:
    AVFormatContext* formatCtx = nullptr;
    AVCodecContext* codecCtx = nullptr;
    AVFrame* frame = nullptr;
    AVPacket* packet = nullptr;
    SwrContext* swrCtx = nullptr; 

    std::shared_ptr<oboe::AudioStream> audioStream;

    int audioStreamIndex = -1;
    int targetSampleRate = 48000;
    int targetChannels = 2; 

    std::atomic<bool> isPlaying;
    std::atomic<bool> isPaused;

    uint8_t* outBuffer = nullptr;
    
    // Naya Background Thread & Buffer Variables
    std::thread decoderThread;
    std::atomic<bool> isDecoding;
    std::mutex bufferMutex;
    std::deque<int16_t> audioBuffer; // Ye tera "Tank" hai
    
    // 2 second ka advance buffer (48000 Hz * 2 Channels * 2 Sec)
    const size_t MAX_BUFFER_SIZE = 48000 * 2 * 2; 

    // Naya background function
    void decodeLoop();
};

#endif // GLOSSY_AUDIO_DECODER_H
