package com.minimalphone.core.common

import android.util.Log

object MinimalLog {
    private const val PREFIX = "MinimalPhone_"

    fun d(tag: String, message: String) {
        runCatching { Log.d("$PREFIX$tag", message) }.onFailure { println("[$PREFIX$tag] $message") }
    }

    fun i(tag: String, message: String) {
        runCatching { Log.i("$PREFIX$tag", message) }.onFailure { println("[$PREFIX$tag] $message") }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        runCatching { Log.w("$PREFIX$tag", message, throwable) }.onFailure { println("[$PREFIX$tag] $message: $throwable") }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        runCatching { Log.e("$PREFIX$tag", message, throwable) }.onFailure { System.err.println("[$PREFIX$tag] $message: $throwable") }
    }
}
