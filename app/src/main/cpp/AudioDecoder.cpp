#include "AudioDecoder.h"

AudioDecoder::AudioDecoder() {
    // Ye sabse zaroori step hai URLs (http/https) ko support karne ke liye
    avformat_network_init();
    frame = av_frame_alloc();
    packet = av_packet_alloc();
}

AudioDecoder::~AudioDecoder() {
    release();
    av_frame_free(&frame);
    av_packet_free(&packet);
    avformat_network_deinit();
}

bool AudioDecoder::openUrl(const std::string& url) {
    LOGI("Opening URL: %s", url.c_str());

    // 1. URL se network stream open karna
    if (avformat_open_input(&formatCtx, url.c_str(), nullptr, nullptr) != 0) {
        LOGE("Network error: URL open nahi hua!");
        return false;
    }

    // 2. Stream ki information nikalna
    if (avformat_find_stream_info(formatCtx, nullptr) < 0) {
        LOGE("Stream info nahi mili!");
        return false;
    }

    // 3. Audio stream dhundhna (video ko ignore karna)
    const AVCodec* codec = nullptr;
    audioStreamIndex = av_find_best_stream(formatCtx, AVMEDIA_TYPE_AUDIO, -1, -1, &codec, 0);
    if (audioStreamIndex < 0 || !codec) {
        LOGE("Koi audio stream ya codec nahi mila!");
        return false;
    }

    // 4. Codec ko setup karna
    codecCtx = avcodec_alloc_context3(codec);
    avcodec_parameters_to_context(codecCtx, formatCtx->streams[audioStreamIndex]->codecpar);
    
    if (avcodec_open2(codecCtx, codec, nullptr) < 0) {
        LOGE("Codec open karne me fail ho gaya!");
        return false;
    }

    // 5. Resampler Setup (Ye YouTube ke alag-alag formats ko Oboe ke liye 48kHz, 16-bit me convert karega)
    swrCtx = swr_alloc();
    
    // Naya FFmpeg 5.0+ API channel layout ke liye
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

    LOGI("Audio Stream successfully open aur setup ho gayi!");
    return true;
}

int AudioDecoder::decodeFrame(uint8_t** outBuffer) {
    // Ye function baad me Oboe ke sath connect hoga raw PCM data bhejne ke liye
    // Abhi ke liye URL open check karne ke liye isko basic rakha hai
    return 0; 
}

void AudioDecoder::release() {
    if (swrCtx) {
        swr_free(&swrCtx);
    }
    if (codecCtx) {
        avcodec_free_context(&codecCtx);
    }
    if (formatCtx) {
        avformat_close_input(&formatCtx);
    }
}
