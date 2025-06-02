package com.n7mobile.onyxtab.helpers

import android.util.Log
import org.threeten.bp.Duration
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class MemoryMonitor(private val interval: Duration, private val thresholdExceedCount: Int) {

    enum class Mode {
        /** Free memory = (maxMemory - totalMemory) */
        TOTAL,
        /** Free memory = maxOf(freeMemory, (maxMemory - totalMemory)) */
        FREE
    }

    sealed class Threshold(open val mode: Mode) {
        data class Percent(override val mode: Mode, val value: Int) : Threshold(mode)
        data class Bytes(override val mode: Mode, val value: Long) : Threshold(mode)
    }

    data class Data(val freeMemory: Long, val totalMemory: Long, val maxMemory: Long)

    companion object {
        private const val TAG = "n7.MemoryMonitor"
    }

    private var executorTask: Future<*>? = null
    private val executor = Executors.newSingleThreadScheduledExecutor(NamedThreadFactory("MemoryMonitor"))

    private var registeredThreshold: Pair<Threshold, () -> Unit>? = null

    private var exceedCount = 0

    val currentMemoryValues: Data
        get() = Runtime.getRuntime().let { Data(it.freeMemory(), it.totalMemory(), it.maxMemory()) }

    private val memoryMonitorTask = Runnable {
        val currentMemory = currentMemoryValues
        printMemoryLog(currentMemory)
        registeredThreshold?.also { checkThresholdExceeded(currentMemory, it) }
    }

    fun start() {
        if (executorTask == null) {
            executorTask = executor.scheduleWithFixedDelay(memoryMonitorTask, 0L, interval.toMillis(), TimeUnit.MILLISECONDS)
        }
    }

    // Ultimate implementation should let us register more than one threshold,
    // and memory monitor should check them independently
    //
    fun registerThreshold(threshold: Threshold, callback: () -> Unit) {
        registeredThreshold = Pair(threshold, callback)
        start()
    }

    fun clear() {
        registeredThreshold = null
        executorTask?.cancel(true)
        executorTask = null
    }

    private fun printMemoryLog(data: Data) {
        Log.d(TAG, "Free: ${data.freeMemory / 1_000_000L} MB; Total: ${data.totalMemory / 1_000_000L} MB; Max: ${data.maxMemory / 1_000_000L} MB")
    }

    private fun checkThresholdExceeded(data: Data, thresholdPair: Pair<Threshold, () -> Unit>) {
        val threshold = thresholdPair.first
        val callback = thresholdPair.second
        val freeBytes = calculateFreeMemory(threshold.mode, data)

        val isThresholdExceeded: Boolean = when (threshold) {
            is Threshold.Percent -> {
                val availablePercent = ((freeBytes.coerceAtLeast(0L) * 100L) / data.maxMemory).toInt()
                (availablePercent < threshold.value).also { exceeded ->
                    if (exceeded) Log.w(TAG, "Threshold.Percent exceeded. Available percent:$availablePercent Threshold value:${threshold.value}")
                }
            }
            is Threshold.Bytes -> (freeBytes < threshold.value).also { exceeded ->
                if (exceeded) Log.w(TAG, "Threshold.Bytes exceeded. Free bytes:${freeBytes} Threshold value:${threshold.value}")
            }
        }

        exceedCount = if (isThresholdExceeded) {
            exceedCount + 1
        } else 0

        if (exceedCount == thresholdExceedCount.coerceAtLeast(1)) {
            Log.e(TAG, "Threshold exceeded $exceedCount times, limit is $thresholdExceedCount")
            callback.invoke()
        }
    }

    private fun calculateFreeMemory(mode: Mode, data: Data): Long =
        when (mode) {
            Mode.FREE -> maxOf(data.maxMemory - data.totalMemory, data.freeMemory)
            Mode.TOTAL -> (data.maxMemory - data.totalMemory)
        }
}