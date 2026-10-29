package com.pelypets.todolist.feature.authgate

import androidx.lifecycle.ViewModel
import com.pelypets.todolist.domain.model.SessionState
import com.pelypets.todolist.domain.usecase.ObserveSessionUseCase
import kotlinx.coroutines.flow.StateFlow

/** Decides between the auth flow and the todo list; the session ends on logout or a failed token refresh. */
class AuthGateViewModel(observeSession: ObserveSessionUseCase) : ViewModel() {
    val session: StateFlow<SessionState> = observeSession()
}
