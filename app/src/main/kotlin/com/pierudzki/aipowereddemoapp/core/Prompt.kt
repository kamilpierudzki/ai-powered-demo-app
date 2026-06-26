package com.pierudzki.aipowereddemoapp.core

import kotlinx.coroutines.flow.Flow

interface Prompt<T> {

    sealed interface Status {
        data class Ready<T>(val value: T) : Status
        data object Processing : Status
    }

    suspend fun execute(): Flow<Status>
}