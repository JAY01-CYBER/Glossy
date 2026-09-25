cmake_minimum_required(VERSION 3.22.1)
project("glossy_audio_engine")

set(CMAKE_CXX_STANDARD 17)

# 1. Pre-built FFmpeg ko import karna (GitHub repo se download ki hui single file)
add_library(ffmpeg SHARED IMPORTED)
set_target_properties(ffmpeg PROPERTIES 
    IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libffmpeg.so
)

# 2. FFmpeg Headers ka rasta
target_include_directories(ffmpeg INTERFACE ${CMAKE_SOURCE_DIR}/include)

# 3. Tera custom Glossy Engine (Yahan AudioDecoder.cpp add kiya hai)
add_library(glossy_engine SHARED 
    glossy_engine.cpp 
    AudioDecoder.cpp
)

# 4. Google Oboe (Hardware Audio Playback)
find_package(oboe REQUIRED CONFIG)

# 5. Sabko ek sath link karna
target_link_libraries(glossy_engine
    ffmpeg
    oboe::oboe
    log
    android
)

# Android 15 ke 16KB Page Size ko support karne ke liye flag
target_link_options(glossy_engine PRIVATE "-Wl,-z,max-page-size=16384")
