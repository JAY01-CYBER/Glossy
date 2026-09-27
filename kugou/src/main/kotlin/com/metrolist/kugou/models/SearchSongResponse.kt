package com.metrolist.kugou.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchSongResponse(
    val status: Int,
    val errcode: Int,
    val error: String,
    val data: Data,
) {
    @Serializable
    data class Data(
        val info: List<Info> = emptyList(),
    ) {
        @Serializable
        data class Info(
            val duration: Int,
            val hash: String,
            val songname: String = "",
            val singername: String = "",
            val filename: String = ""
        )
    }
}
