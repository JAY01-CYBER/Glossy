package com.jay.glossy.extensions

import java.io.File

fun File.directorySizeBytes(): Long {
    var size: Long = 0
    if (this.isDirectory) {
        this.listFiles()?.forEach { child ->
            size += child.directorySizeBytes()
        }
    } else {
        size = this.length()
    }
    return size
}
