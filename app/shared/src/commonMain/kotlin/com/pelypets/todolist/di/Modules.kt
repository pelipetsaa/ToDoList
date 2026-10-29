package com.pelypets.todolist.di

import com.pelypets.todolist.data.local.KVaultTokenStorage
import com.pelypets.todolist.data.local.TokenStorage
import com.pelypets.todolist.data.remote.ApiConfig
import com.pelypets.todolist.data.remote.AuthApi
import com.pelypets.todolist.data.remote.HttpClientFactory
import com.pelypets.todolist.data.remote.TodoApi
import com.pelypets.todolist.data.repository.AuthRepositoryImpl
import com.pelypets.todolist.data.repository.TodoRepositoryImpl
import com.pelypets.todolist.data.session.SessionManager
import com.pelypets.todolist.domain.period.PeriodRangeCalculator
import com.pelypets.todolist.domain.repository.AuthRepository
import com.pelypets.todolist.domain.repository.SessionRepository
import com.pelypets.todolist.domain.repository.TodoRepository
import com.pelypets.todolist.domain.usecase.DefaultDueDateUseCase
import com.pelypets.todolist.domain.usecase.DeleteTodoUseCase
import com.pelypets.todolist.domain.usecase.GetTodoUseCase
import com.pelypets.todolist.domain.usecase.GetTodosForPeriodUseCase
import com.pelypets.todolist.domain.usecase.LoginWithEmailUseCase
import com.pelypets.todolist.domain.usecase.LoginWithGoogleUseCase
import com.pelypets.todolist.domain.usecase.LogoutUseCase
import com.pelypets.todolist.domain.usecase.ObserveSessionUseCase
import com.pelypets.todolist.domain.usecase.RegisterUseCase
import com.pelypets.todolist.domain.usecase.SaveTodoUseCase
import com.pelypets.todolist.feature.auth.login.LoginViewModel
import com.pelypets.todolist.feature.auth.register.RegisterViewModel
import com.pelypets.todolist.feature.authgate.AuthGateViewModel
import com.pelypets.todolist.feature.tododetail.TodoDetailViewModel
import com.pelypets.todolist.feature.todolist.TodoListViewModel
import kotlinx.datetime.TimeZone
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.Clock

fun dataModule(apiConfig: ApiConfig): Module = module {
    single { apiConfig }
    single<TokenStorage> { KVaultTokenStorage(get()) }
    single { SessionManager(get()) } bind SessionRepository::class
    single { HttpClientFactory.create(get(), get(), get()) }
    factoryOf(::AuthApi)
    factoryOf(::TodoApi)
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
    single<TodoRepository> { TodoRepositoryImpl(get()) }
}

val domainModule: Module = module {
    single<Clock> { Clock.System }
    // The user may change the device time zone while the app is running.
    factory { TimeZone.currentSystemDefault() }
    factoryOf(::PeriodRangeCalculator)
    factoryOf(::LoginWithEmailUseCase)
    factoryOf(::RegisterUseCase)
    factoryOf(::LoginWithGoogleUseCase)
    factoryOf(::LogoutUseCase)
    factoryOf(::ObserveSessionUseCase)
    factoryOf(::GetTodosForPeriodUseCase)
    factoryOf(::GetTodoUseCase)
    factoryOf(::SaveTodoUseCase)
    factoryOf(::DeleteTodoUseCase)
    factoryOf(::DefaultDueDateUseCase)
}

val presentationModule: Module = module {
    viewModelOf(::AuthGateViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
    viewModelOf(::TodoListViewModel)
    viewModel { params ->
        TodoDetailViewModel(
            todoId = params.getOrNull(),
            getTodo = get(),
            saveTodo = get(),
            deleteTodo = get(),
            defaultDueDate = get(),
            timeZone = get(),
        )
    }
}
