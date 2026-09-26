#include "AudioDecoder.h"
#include <algorithm>

AudioDecoder::AudioDecoder() {
    avformat_network_init();
    frame = av_frame_alloc();
    packet = av_packet_alloc();
    
    // Allocate 1 second buffer for output (48000 samples * 2 channels * 2 bytes)
    av_samples_alloc(&outBuffer, nullptr, targetChannels, 48000, AV_SAMPLE_FMT_S16, 0);
}

AudioDecoder::~AudioDecoder() {
    stop();
    if (outBuffer) {
        av_freep(&outBuffer);
    }
    av_frame_free(&frame);
    av_packet_free(&packet);
    avformat_network_deinit();
}

bool AudioDecoder::openUrl(const std::string& url) {
    LOGI("Opening URL in FFmpeg: %s", url.c_str());

    if (avformat_open_input(&formatCtx, url.c_str(), nullptr, nullptr) != 0) {
        LOGE("Network error: URL open nahi hua!");
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
    
    if (avcodec_open2(codecCtx, codec, nullptr) < 0) {
        LOGE("Codec open karne me fail ho gaya!");
        return false;
    }

    // Resampler setup: Kisi bhi audio format ko 48kHz Stereo 16-bit PCM me convert karne ke liye
    swrCtx = swr_alloc();
    av_opt_set_chlayout(swrCtx, "in_chlayout", &codecCtx->ch_layout, 0);
    av_opt_set_int(swrCtx, "in_sample_rate", codecCtx->sample_rate, 0);
    av_opt_set_sample_fmt(swrCtx, "in_sample_fmt", codecCtx->sample_fmt, 0);
    
    AVChannelLayout outLayout;
    av_channel_layout_default(&outLayout, targetChannels);
    av_opt_set_chlayout(swrCtx, "out_chlayout", &outLayout, 0);
    av_opt_set_int(swrCtx, "out_sample_rate", targetSampleRate, 0);
    av_opt_set_sample_fmt(swrCtx, "out_sample_fmt", AV_SAMPLE_FMT_S16, 0);

    if (swr_init(swrCtx) < 0) {
        LOGE("Resampler initialize nahi hua!");
        return false;
    }

    outBufferSize = 0;
    outBufferIndex = 0;

    // 🔥 OBOE HARDWARE AUDIO SETUP 🔥
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
           ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
           ->setSharingMode(oboe::SharingMode::Shared)
           ->setFormat(oboe::AudioFormat::I16)
           ->setChannelCount(targetChannels)
           ->setSampleRate(targetSampleRate)
           ->setDataCallback(this); // Engine khud callback lega

    oboe::Result result = builder.openStream(audioStream);
    if (result != oboe::Result::OK) {
        LOGE("Oboe stream kholne me fail: %s", oboe::convertToText(result));
        return false;
    }

    // Oboe Audio Playback Start!
    audioStream->requestStart();
    
    isPlaying = true;
    isPaused = false;
    LOGI("Audio Stream open aur Oboe Hardware Playback start ho gaya! 🎵");
    return true;
}

void AudioDecoder::pause() {
    if (isPlaying && !isPaused) {
        isPaused = true;
        if (audioStream) audioStream->requestPause();
        LOGI("Glossy Decoder: Playback PAUSED");
    }
}

void AudioDecoder::resume() {
    if (isPlaying && isPaused) {
        isPaused = false;
        if (audioStream) audioStream->requestStart();
        LOGI("Glossy Decoder: Playback RESUMED");
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
    LOGI("Glossy Decoder: Playback STOPPED");
}

int AudioDecoder::decodeNextFrame() {
    int ret = av_read_frame(formatCtx, packet);
    if (ret < 0) return ret; // End of file or error

    if (packet->stream_index == audioStreamIndex) {
        ret = avcodec_send_packet(codecCtx, packet);
        if (ret == 0) {
            ret = avcodec_receive_frame(codecCtx, frame);
            if (ret == 0) {
                // Resample and convert to 16-bit PCM
                int out_samples = swr_convert(swrCtx, &outBuffer, frame->nb_samples,
                                              (const uint8_t**)frame->data, frame->nb_samples);
                outBufferSize = out_samples * targetChannels * sizeof(int16_t);
                outBufferIndex = 0;
            }
        }
    }
    av_packet_unref(packet);
    return 0;
}

// 🔥 YAHI WO FUNCTION HAI JO PHONE KA HARDWARE HAR MILLISECOND CALL KARTA HAI 🔥
oboe::DataCallbackResult AudioDecoder::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
    int16_t *outputBuffer = static_cast<int16_t *>(audioData);
    int framesToFill = numFrames;
    int framesFilled = 0;

    // Agar pause hai ya stream read nahi karni, toh shanti (silence) bhejo speaker me
    if (!isPlaying || isPaused) {
        memset(audioData, 0, numFrames * targetChannels * sizeof(int16_t));
        return oboe::DataCallbackResult::Continue;
    }

    while (framesToFill > 0) {
        if (outBufferIndex >= outBufferSize) {
            // Buffer khali hai, naya data FFmpeg se fetch karo
            int ret = decodeNextFrame();
            if (ret < 0) {
                // Gaana khatam
                memset(outputBuffer + (framesFilled * targetChannels), 0, framesToFill * targetChannels * sizeof(int16_t));
                break;
            }
            if (outBufferSize == 0) continue; // Skip video/empty packets
        }

        int bytesAvailable = outBufferSize - outBufferIndex;
        int framesAvailable = bytesAvailable / (targetChannels * sizeof(int16_t));

        int framesToCopy = std::min(framesToFill, framesAvailable);
        int bytesToCopy = framesToCopy * targetChannels * sizeof(int16_t);

        // FFmpeg Buffer se Oboe Hardware Buffer me copy
        memcpy(outputBuffer + (framesFilled * targetChannels), outBuffer + outBufferIndex, bytesToCopy);

        outBufferIndex += bytesToCopy;
        framesFilled += framesToCopy;
        framesToFill -= framesToCopy;
    }

    return oboe::DataCallbackResult::Continue;
}

void AudioDecoder::release() {
    if (swrCtx) {
        swr_free(&swrCtx);
        swrCtx = nullptr;
    }
    if (codecCtx) {
        avcodec_free_context(&codecCtx);
        codecCtx = nullptr;
    }
    if (formatCtx) {
        avformat_close_input(&formatCtx);
        formatCtx = nullptr;
    }
}
