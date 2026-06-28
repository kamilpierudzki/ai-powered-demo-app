package com.pierudzki.aipowereddemoapp.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pierudzki.aipowereddemoapp.ai.action.Action
import com.pierudzki.aipowereddemoapp.ai.answer.Answer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BrainViewModel(application: Application) : AndroidViewModel(application) {

    private val brain = Brain()
    val answer: StateFlow<Answer> = brain.answer

    fun onNewInputAction(action: Action) {
        viewModelScope.launch {
            brain.onNewInputAction(action)
        }
    }
}
