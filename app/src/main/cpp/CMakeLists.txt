cmake_minimum_required(VERSION 3.22.1)
project("glossy_audio_engine")

set(CMAKE_CXX_STANDARD 17)

# 1. Prefab FFmpeg Import karna (Maven Central se)
find_package(ffmpeg REQUIRED CONFIG)

# 2. Tera custom Glossy Engine
add_library(glossy_engine SHARED 
    glossy_engine.cpp 
    AudioDecoder.cpp
)

# 3. Google Oboe (Hardware Audio Playback)
find_package(oboe REQUIRED CONFIG)

# 4. Sabko ek sath link karna
target_link_libraries(glossy_engine
    ffmpeg::ffmpeg
    oboe::oboe
    log
    android
)

# Android 15 ke 16KB Page Size ko support karne ke liye flag
target_link_options(glossy_engine PRIVATE "-Wl,-z,max-page-size=16384")
