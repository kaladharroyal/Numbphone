package com.minimalphone.core.common

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: MinimalDispatchers)

enum class MinimalDispatchers {
    Default,
    IO,
    Main
}

data class MinimalAppDispatchers(
    val io: CoroutineDispatcher = Dispatchers.IO,
    val default: CoroutineDispatcher = Dispatchers.Default,
    val main: CoroutineDispatcher = Dispatchers.Main
)

sealed interface MinimalResult<out T> {
    data class Success<T>(val data: T) : MinimalResult<T>
    data class Error(val exception: Throwable, val message: String? = exception.message) : MinimalResult<Nothing>
    data object Loading : MinimalResult<Nothing>
}

object MinimalLog {
    private const val PREFIX = "MinimalPhone_"

    fun d(tag: String, message: String) {
        Log.d("$PREFIX$tag", message)
    }

    fun i(tag: String, message: String) {
        Log.i("$PREFIX$tag", message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w("$PREFIX$tag", message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$PREFIX$tag", message, throwable)
    }
}
