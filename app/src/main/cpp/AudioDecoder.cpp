#include "AudioDecoder.h"
#include <algorithm>

AudioDecoder::AudioDecoder() {
    avformat_network_init();
    frame = av_frame_alloc();
    packet = av_packet_alloc();
    
    // Allocate buffer
    av_samples_alloc(&outBuffer, nullptr, targetChannels, 48000, AV_SAMPLE_FMT_S16, 0);
}

AudioDecoder::~AudioDecoder() {
    stop();
    if (outBuffer) av_freep(&outBuffer);
    av_frame_free(&frame);
    av_packet_free(&packet);
    avformat_network_deinit();
}

bool AudioDecoder::openUrl(const std::string& url) {
    LOGI("Opening URL in FFmpeg: %s", url.c_str());

    // NOTE: Agar tera prebuilt FFmpeg bina SSL ke compile hua hai, toh HTTPS links yahan fail ho jayenge.
    if (avformat_open_input(&formatCtx, url.c_str(), nullptr, nullptr) != 0) {
        LOGE("Network error: URL open nahi hua! (HTTPS issue ho sakta hai)");
        return false;
    }

    if (avformat_find_stream_info(formatCtx, nullptr) < 0) {
        LOGE("Stream info nahi mili!");
        return false;
    }

    const AVCodec* codec = nullptr;
    audioStreamIndex = av_find_best_stream(formatCtx, AVMEDIA_TYPE_AUDIO, -1, -1, &codec, 0);
    if (audioStreamIndex < 0 || !codec) {
        LOGE("Koi audio stream ya codec nahi mila!");
        return false;
    }

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

    outBufferSize = 0;
    outBufferIndex = 0;

    // OBOE SETUP
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
           ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
           ->setSharingMode(oboe::SharingMode::Shared)
           ->setFormat(oboe::AudioFormat::I16)
           ->setChannelCount(targetChannels)
           ->setSampleRate(targetSampleRate)
           ->setDataCallback(this);

    oboe::Result result = builder.openStream(audioStream);
    if (result != oboe::Result::OK) return false;

    audioStream->requestStart();
    isPlaying = true;
    isPaused = false;
    LOGI("Audio Stream open aur Oboe start ho gaya! 🎵");
    return true;
}

void AudioDecoder::pause() {
    if (isPlaying && !isPaused) {
        isPaused = true;
        if (audioStream) audioStream->requestPause();
    }
}

void AudioDecoder::resume() {
    if (isPlaying && isPaused) {
        isPaused = false;
        if (audioStream) audioStream->requestStart();
    }
}

void AudioDecoder::stop() {
    isPlaying = false;
    isPaused = false;
    if (audioStream) {
        audioStream->requestStop();
        audioStream->close();
        audioStream.reset();
    }
    release();
}

// 🔥 BUG FIX: Ab ye function tab tak loop karega jab tak isko asali audio data na mil jaye
int AudioDecoder::decodeNextFrame() {
    outBufferSize = 0;
    outBufferIndex = 0;

    while (true) {
        int ret = av_read_frame(formatCtx, packet);
        if (ret < 0) return ret; // End of File ya Error

        if (packet->stream_index == audioStreamIndex) {
            ret = avcodec_send_packet(codecCtx, packet);
            if (ret >= 0) {
                ret = avcodec_receive_frame(codecCtx, frame);
                if (ret >= 0) {
                    int out_samples = swr_convert(swrCtx, &outBuffer, frame->nb_samples,
                                                  (const uint8_t**)frame->data, frame->nb_samples);
                    if (out_samples > 0) {
                        outBufferSize = out_samples * targetChannels * sizeof(int16_t);
                        av_packet_unref(packet);
                        return 0; // Success! Audio data mil gaya.
                    }
                }
            }
        }
        // Agar packet audio nahi tha (e.g. cover art), toh usko free karke agla try karo
        av_packet_unref(packet);
    }
    return -1;
}

// 🔥 BUG FIX: Infinite loop issue fixed in onAudioReady
oboe::DataCallbackResult AudioDecoder::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
    int16_t *outputBuffer = static_cast<int16_t *>(audioData);
    int framesToFill = numFrames;
    int framesFilled = 0;

    if (!isPlaying || isPaused) {
        memset(audioData, 0, numFrames * targetChannels * sizeof(int16_t));
        return oboe::DataCallbackResult::Continue;
    }

    while (framesToFill > 0) {
        if (outBufferIndex >= outBufferSize) {
            int ret = decodeNextFrame();
            // Agar gaana khatam ho gaya ya error aaya, toh bache hue buffer ko silence se bhar do
            if (ret < 0 || outBufferSize == 0) {
                memset(outputBuffer + (framesFilled * targetChannels), 0, framesToFill * targetChannels * sizeof(int16_t));
                break; 
            }
        }

        int bytesAvailable = outBufferSize - outBufferIndex;
        int framesAvailable = bytesAvailable / (targetChannels * sizeof(int16_t));

        int framesToCopy = std::min(framesToFill, framesAvailable);
        int bytesToCopy = framesToCopy * targetChannels * sizeof(int16_t);

        memcpy(outputBuffer + (framesFilled * targetChannels), outBuffer + outBufferIndex, bytesToCopy);

        outBufferIndex += bytesToCopy;
        framesFilled += framesToCopy;
        framesToFill -= framesToCopy;
    }

    return oboe::DataCallbackResult::Continue;
}

void AudioDecoder::release() {
    if (swrCtx) { swr_free(&swrCtx); swrCtx = nullptr; }
    if (codecCtx) { avcodec_free_context(&codecCtx); codecCtx = nullptr; }
    if (formatCtx) { avformat_close_input(&formatCtx); formatCtx = nullptr; }
}
