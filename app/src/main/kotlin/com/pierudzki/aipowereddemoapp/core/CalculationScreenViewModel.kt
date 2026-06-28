package com.pierudzki.aipowereddemoapp.core

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

class CalculationScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val _calculationDurationSeconds = MutableStateFlow<Int>(0)
    val calculationDurationSeconds: StateFlow<Int> = _calculationDurationSeconds.asStateFlow()

    private val _values = MutableStateFlow<List<String>>(emptyList())
    val values: StateFlow<List<String>> = _values.asStateFlow()

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished.asStateFlow()

    private var timerJob: Job? = null
    private var calculationJob: Job? = null

    fun startCalculation(n: Int) {
        val startedAt = SystemClock.elapsedRealtime()
        _calculationDurationSeconds.value = 0
        _values.value = emptyList()
        _isFinished.value = false

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000.milliseconds)
                _calculationDurationSeconds.value =
                    ((SystemClock.elapsedRealtime() - startedAt) / 1000).toInt()
            }
        }

        calculationJob?.cancel()
        calculationJob = viewModelScope.launch(Dispatchers.Default) {
            (0 until n).forEach { i ->
                val result = fib(i)
                withContext(Dispatchers.Main) {
                    _values.update { listOf("$i: ${result.toInt()}") + it }
                }
            }
            timerJob?.cancel()
            _calculationDurationSeconds.value =
                ((SystemClock.elapsedRealtime() - startedAt) / 1000).toInt()
            _isFinished.value = true
        }
    }

    fun stopCalculation() {
        timerJob?.cancel()
        calculationJob?.cancel()
    }

    private fun fib(n: Int): Long {
        require(n >= 0) { "n must be non-negative" }
        return if (n < 2) n.toLong() else fib(n - 1) + fib(n - 2)
    }
}
