#include "AudioDecoder.h"
#include <algorithm>

extern "C" {
#include <libavutil/dict.h>
}

// 🛑 NAYA: Jabardasti internet download rokne ka callback
static int decode_interrupt_cb(void *ctx) {
    AudioDecoder* decoder = static_cast<AudioDecoder*>(ctx);
    return decoder->shouldInterrupt() ? 1 : 0;
}

AudioDecoder::AudioDecoder() {
    avformat_network_init();
    frame = av_frame_alloc();
    packet = av_packet_alloc();
    
    av_samples_alloc(&outBuffer, nullptr, targetChannels, 48000, AV_SAMPLE_FMT_S16, 0);
    
    isPlaying = false;
    isPaused = false;
    isDecoding = false;
}

AudioDecoder::~AudioDecoder() {
    stop();
    if (outBuffer) av_freep(&outBuffer);
    av_frame_free(&frame);
    av_packet_free(&packet);
    avformat_network_deinit();
}

bool AudioDecoder::shouldInterrupt() {
    return !isDecoding; // Agar gaana stop ho gaya, toh FFmpeg download turant kaat do
}

bool AudioDecoder::openUrl(const std::string& url) {
    LOGI("Opening URL in FFmpeg: %s", url.c_str());

    isDecoding = true; // Isko pehle true karna zaroori hai

    // 🛑 NAYA: Interrupt callback attach karo taaki Next song pe app freeze na ho
    formatCtx = avformat_alloc_context();
    formatCtx->interrupt_callback.callback = decode_interrupt_cb;
    formatCtx->interrupt_callback.opaque = this;

    AVDictionary* options = nullptr;
    av_dict_set(&options, "user_agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36", 0);
    av_dict_set(&options, "protocol_whitelist", "file,http,https,tcp,tls,crypto", 0);

    int result = avformat_open_input(&formatCtx, url.c_str(), nullptr, &options);
    av_dict_free(&options);

    if (result != 0) {
        LOGE("Network error! FFmpeg Error Code: %d", result);
        return false;
    }

    if (avformat_find_stream_info(formatCtx, nullptr) < 0) return false;

    const AVCodec* codec = nullptr;
    audioStreamIndex = av_find_best_stream(formatCtx, AVMEDIA_TYPE_AUDIO, -1, -1, &codec, 0);
    if (audioStreamIndex < 0 || !codec) return false;

    codecCtx = avcodec_alloc_context3(codec);
    avcodec_parameters_to_context(codecCtx, formatCtx->streams[audioStreamIndex]->codecpar);
    
    if (avcodec_open2(codecCtx, codec, nullptr) < 0) return false;

    swrCtx = swr_alloc();
    av_opt_set_chlayout(swrCtx, "in_chlayout", &codecCtx->ch_layout, 0);
    av_opt_set_int(swrCtx, "in_sample_rate", codecCtx->sample_rate, 0);
    av_opt_set_sample_fmt(swrCtx, "in_sample_fmt", codecCtx->sample_fmt, 0);
    
    AVChannelLayout outLayout;
    av_channel_layout_default(&outLayout, targetChannels);
    av_opt_set_chlayout(swrCtx, "out_chlayout", &outLayout, 0);
    av_opt_set_int(swrCtx, "out_sample_rate", targetSampleRate, 0);
    av_opt_set_sample_fmt(swrCtx, "out_sample_fmt", AV_SAMPLE_FMT_S16, 0);

    if (swr_init(swrCtx) < 0) return false;

    {
        std::lock_guard<std::mutex> lock(bufferMutex);
        audioBuffer.clear();
    }

    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
           ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
           ->setSharingMode(oboe::SharingMode::Shared)
           ->setFormat(oboe::AudioFormat::I16)
           ->setChannelCount(targetChannels)
           ->setSampleRate(targetSampleRate)
           ->setDataCallback(this);

    oboe::Result res = builder.openStream(audioStream);
    if (res != oboe::Result::OK) return false;

    isPlaying = true;
    isPaused = false;
    
    decoderThread = std::thread(&AudioDecoder::decodeLoop, this);
    
    audioStream->requestStart();
    return true;
}

void AudioDecoder::pause() {
    isPaused = true;
    if (audioStream && audioStream->getState() == oboe::StreamState::Started) {
        audioStream->requestPause();
    }
}

void AudioDecoder::resume() {
    isPaused = false;
    if (audioStream && audioStream->getState() != oboe::StreamState::Started) {
        audioStream->requestStart();
    }
}

void AudioDecoder::stop() {
    isDecoding = false; // Turant FFmpeg download kill karega
    isPlaying = false;
    isPaused = false;
    
    if (decoderThread.joinable()) {
        decoderThread.join();
    }

    if (audioStream) {
        audioStream->requestStop();
        audioStream->close();
        audioStream.reset();
    }

    {
        std::lock_guard<std::mutex> lock(bufferMutex);
        audioBuffer.clear();
    }
    release();
}

void AudioDecoder::decodeLoop() {
    while (isDecoding) {
        bool isFull = false;
        {
            std::lock_guard<std::mutex> lock(bufferMutex);
            isFull = (audioBuffer.size() >= MAX_BUFFER_SIZE);
        }

        if (isFull) {
            std::this_thread::sleep_for(std::chrono::milliseconds(20));
            continue;
        }

        int ret = av_read_frame(formatCtx, packet);
        if (ret < 0) {
            std::this_thread::sleep_for(std::chrono::milliseconds(50));
            continue; 
        }

        if (packet->stream_index == audioStreamIndex) {
            ret = avcodec_send_packet(codecCtx, packet);
            if (ret >= 0) {
                ret = avcodec_receive_frame(codecCtx, frame);
                if (ret >= 0) {
                    int out_samples = swr_convert(swrCtx, &outBuffer, frame->nb_samples,
                                                  (const uint8_t**)frame->data, frame->nb_samples);
                    if (out_samples > 0) {
                        int totalSamples = out_samples * targetChannels;
                        int16_t* pcmData = (int16_t*)outBuffer;
                        
                        std::lock_guard<std::mutex> lock(bufferMutex);
                        audioBuffer.insert(audioBuffer.end(), pcmData, pcmData + totalSamples);
                    }
                }
            }
        }
        av_packet_unref(packet);
    }
}

oboe::DataCallbackResult AudioDecoder::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
    int16_t *outputBuffer = static_cast<int16_t *>(audioData);
    int samplesNeeded = numFrames * targetChannels;

    if (!isPlaying || isPaused) {
        memset(audioData, 0, samplesNeeded * sizeof(int16_t));
        return oboe::DataCallbackResult::Continue;
    }

    std::lock_guard<std::mutex> lock(bufferMutex);
    if (audioBuffer.size() >= samplesNeeded) {
        std::copy(audioBuffer.begin(), audioBuffer.begin() + samplesNeeded, outputBuffer);
        audioBuffer.erase(audioBuffer.begin(), audioBuffer.begin() + samplesNeeded);
    } else {
        int available = audioBuffer.size();
        if (available > 0) {
            std::copy(audioBuffer.begin(), audioBuffer.end(), outputBuffer);
            audioBuffer.clear();
        }
        memset(outputBuffer + available, 0, (samplesNeeded - available) * sizeof(int16_t));
    }
    return oboe::DataCallbackResult::Continue;
}

void AudioDecoder::release() {
    if (swrCtx) { swr_free(&swrCtx); swrCtx = nullptr; }
    if (codecCtx) { avcodec_free_context(&codecCtx); codecCtx = nullptr; }
    if (formatCtx) { avformat_close_input(&formatCtx); formatCtx = nullptr; }
}
