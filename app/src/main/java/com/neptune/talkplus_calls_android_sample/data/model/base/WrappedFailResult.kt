package com.neptune.talkplus_calls_android_sample.data.model.base

data class WrappedFailResult (
    val stateName: String,
    val errorCode: Int,
    val exception: Exception
)