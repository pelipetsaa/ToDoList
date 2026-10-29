package com.pelypets.todolist.data.session

import com.pelypets.todolist.data.local.StoredTokens
import com.pelypets.todolist.data.local.TokenStorage
import com.pelypets.todolist.domain.model.SessionState
import com.pelypets.todolist.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(private val storage: TokenStorage) : SessionRepository {

    private val _session = MutableStateFlow(
        if (storage.read() != null) SessionState.LOGGED_IN else SessionState.LOGGED_OUT,
    )
    override val session: StateFlow<SessionState> = _session.asStateFlow()

    fun currentTokens(): StoredTokens? = storage.read()

    fun start(tokens: StoredTokens) {
        storage.save(tokens)
        _session.value = SessionState.LOGGED_IN
    }

    fun end() {
        storage.clear()
        _session.value = SessionState.LOGGED_OUT
    }
}
