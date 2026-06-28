package com.pierudzki.aipowereddemoapp.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pierudzki.aipowereddemoapp.ai.action.Action
import com.pierudzki.aipowereddemoapp.ai.answer.Answer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BrainViewModel(application: Application) : AndroidViewModel(application) {

    private val brain = Brain()
    val answer: StateFlow<Answer> = brain.answer

    private val brainMutex = Mutex()

    fun onNewInputAction(action: Action) {
        viewModelScope.launch {
            if (action.isDroppableWhenBusy) {
                if (!brainMutex.tryLock()) return@launch
                try {
                    brain.onNewInputAction(action)
                } finally {
                    brainMutex.unlock()
                }
            } else {
                brainMutex.withLock {
                    brain.onNewInputAction(action)
                }
            }
        }
    }
}
