package com.prslc.zhiflow.di

import android.content.Context
import android.content.SharedPreferences
import coil3.SingletonImageLoader
import com.prslc.zhiflow.core.network.ApiClient
import com.prslc.zhiflow.core.network.CredentialStore
import com.prslc.zhiflow.core.network.HttpClientProvider
import com.prslc.zhiflow.core.network.HttpLogStore
import com.prslc.zhiflow.core.network.SessionCookieJar
import com.prslc.zhiflow.core.network.SessionGate
import com.prslc.zhiflow.core.preference.LanguageStore
import com.prslc.zhiflow.core.preference.LanguageStoreImpl
import com.prslc.zhiflow.core.preference.SettingsStore
import com.prslc.zhiflow.core.preference.SettingsStoreImpl
import com.prslc.zhiflow.data.remote.service.ActionService
import com.prslc.zhiflow.data.remote.service.AuthService
import com.prslc.zhiflow.data.remote.service.CollectionService
import com.prslc.zhiflow.data.remote.service.CommentService
import com.prslc.zhiflow.data.remote.service.ContentService
import com.prslc.zhiflow.data.remote.service.FeedService
import com.prslc.zhiflow.data.remote.service.FeedbackService
import com.prslc.zhiflow.data.remote.service.MomentService
import com.prslc.zhiflow.data.remote.service.QuestionService
import com.prslc.zhiflow.data.remote.service.ReadHistoryService
import com.prslc.zhiflow.data.remote.service.UserService
import com.prslc.zhiflow.data.repository.ActionRepository
import com.prslc.zhiflow.data.repository.AuthRepository
import com.prslc.zhiflow.data.repository.CollectionRepository
import com.prslc.zhiflow.data.repository.CommentRepository
import com.prslc.zhiflow.data.repository.ContentRepository
import com.prslc.zhiflow.data.repository.FeedRepository
import com.prslc.zhiflow.data.repository.FeedbackRepository
import com.prslc.zhiflow.data.repository.MomentRepository
import com.prslc.zhiflow.data.repository.QuestionRepository
import com.prslc.zhiflow.data.repository.ReadHistoryRepository
import com.prslc.zhiflow.data.repository.UserRepository
import com.prslc.zhiflow.data.session.CredentialStoreImpl
import com.prslc.zhiflow.data.session.SessionManager
import com.prslc.zhiflow.data.session.UserSession
import com.prslc.zhiflow.ui.component.widget.ImageLightboxController
import com.prslc.zhiflow.ui.page.collection.CollectionContentsViewModel
import com.prslc.zhiflow.ui.page.comment.CommentViewModel
import com.prslc.zhiflow.ui.page.content.CollectionViewModel
import com.prslc.zhiflow.ui.page.content.ContentViewModel
import com.prslc.zhiflow.ui.page.debug.DebugViewModel
import com.prslc.zhiflow.ui.page.debug.HttpLogViewModel
import com.prslc.zhiflow.ui.page.feed.FeedViewModel
import com.prslc.zhiflow.ui.page.feedback.FeedbackBlockListViewModel
import com.prslc.zhiflow.ui.page.history.ReadHistoryViewModel
import com.prslc.zhiflow.ui.page.login.LoginViewModel
import com.prslc.zhiflow.ui.page.people.PeopleViewModel
import com.prslc.zhiflow.ui.page.people.moment.ActivitiesViewModel
import com.prslc.zhiflow.ui.page.people.moment.PostsViewModel
import com.prslc.zhiflow.ui.page.people.moment.UpvotesViewModel
import com.prslc.zhiflow.ui.page.pin.PinViewModel
import com.prslc.zhiflow.ui.page.profile.ProfileViewModel
import com.prslc.zhiflow.ui.page.profile.SettingsViewModel
import com.prslc.zhiflow.ui.page.question.QuestionViewModel
import okhttp3.CookieJar
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // SharedPreferences
    single<SharedPreferences> {
        androidContext().getSharedPreferences("temp_auth_prefs", Context.MODE_PRIVATE)
    }

    singleOf(::HttpLogStore)
    single<CookieJar> { SessionCookieJar(get()) }
    singleOf(::HttpClientProvider)
    single { get<HttpClientProvider>().okHttpClient }
    // The same loader `AsyncImage` reaches through SingletonImageLoader, registered so what needs
    // the cache itself -- the settings page -- can be handed it instead of a Context.
    single { SingletonImageLoader.get(androidContext()) }
    single { ApiClient(get()) { get() } }
    singleOf(::ImageLightboxController)

    // Settings
    single<SettingsStore> { SettingsStoreImpl(get()) }
    single<LanguageStore> { LanguageStoreImpl(androidContext()) }

    // Session
    single<CredentialStore> { CredentialStoreImpl(get()) }
    // Bound under both types: the funnel asks for the gate, the startup path asks for the
    // manager's prefetch, and both have to be the same instance.
    singleOf(::SessionManager)
    single<SessionGate> { get<SessionManager>() }

    // Account
    singleOf(::AuthService)
    singleOf(::AuthRepository)
    viewModelOf(::LoginViewModel)

    // Feed
    singleOf(::FeedService)
    singleOf(::FeedRepository)
    viewModelOf(::FeedViewModel)

    // Collection
    singleOf(::CollectionService)
    singleOf(::CollectionRepository)
    viewModelOf(::CollectionViewModel)
    viewModelOf(::CollectionContentsViewModel)

    // Comment
    singleOf(::CommentService)
    singleOf(::CommentRepository)
    viewModelOf(::CommentViewModel)

    // User
    singleOf(::UserService)
    singleOf(::UserSession)
    singleOf(::UserRepository)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::SettingsViewModel)

    // Content
    singleOf(::ContentService)
    singleOf(::ContentRepository)
    viewModelOf(::ContentViewModel)
    viewModelOf(::PinViewModel)

    // Question
    singleOf(::QuestionService)
    singleOf(::QuestionRepository)
    viewModelOf(::QuestionViewModel)

    // Action
    singleOf(::ActionService)
    singleOf(::ActionRepository)

    // Negative feedback
    singleOf(::FeedbackService)
    singleOf(::FeedbackRepository)
    viewModelOf(::FeedbackBlockListViewModel)

    // ReadHistory
    singleOf(::ReadHistoryService)
    singleOf(::ReadHistoryRepository)
    viewModelOf(::ReadHistoryViewModel)

    // Moment
    singleOf(::MomentService)
    singleOf(::MomentRepository)
    viewModelOf(::PostsViewModel)
    viewModelOf(::ActivitiesViewModel)
    viewModelOf(::UpvotesViewModel)

    // People
    viewModelOf(::PeopleViewModel)

    // debug
    viewModelOf(::DebugViewModel)
    viewModelOf(::HttpLogViewModel)
}
