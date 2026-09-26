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
    
    // Naye seek aur volume functions
    void seekTo(int64_t positionMs);
    void setVolume(float vol);

    bool shouldInterrupt();

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
    
    // Thread-safe variables for seek and volume
    std::atomic<bool> seekRequested{false};
    std::atomic<int64_t> seekTargetMs{0};
    std::atomic<float> volume{1.0f};

    uint8_t* outBuffer = nullptr;
    
    std::thread decoderThread;
    std::atomic<bool> isDecoding;
    std::mutex bufferMutex;
    std::deque<int16_t> audioBuffer;
    
    const size_t MAX_BUFFER_SIZE = 48000 * 2 * 2; 

    void decodeLoop();
};

#endif // GLOSSY_AUDIO_DECODER_H
