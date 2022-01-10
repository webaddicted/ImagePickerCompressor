package com.webaddicted.imagepickercompressor.utils.common

interface DownloadListener {
    fun onSuccess(path: String)
    fun onFailure(error: String)
}