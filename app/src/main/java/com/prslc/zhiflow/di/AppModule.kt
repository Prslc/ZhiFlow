package com.prslc.zhiflow.di

import android.content.Context
import android.content.SharedPreferences
import com.prslc.zhiflow.core.network.Client
import com.prslc.zhiflow.core.network.HeaderProvider
import com.prslc.zhiflow.data.repository.ActionRepository
import com.prslc.zhiflow.data.repository.CollectionRepository
import com.prslc.zhiflow.data.repository.CommentRepository
import com.prslc.zhiflow.data.repository.ContentRepository
import com.prslc.zhiflow.data.repository.FeedRepository
import com.prslc.zhiflow.data.repository.MomentRepository
import com.prslc.zhiflow.data.repository.QuestionRepository
import com.prslc.zhiflow.data.repository.UserRepository
import com.prslc.zhiflow.data.service.ActionService
import com.prslc.zhiflow.data.service.CollectionService
import com.prslc.zhiflow.data.service.CommentService
import com.prslc.zhiflow.data.service.ContentService
import com.prslc.zhiflow.data.service.FeedService
import com.prslc.zhiflow.data.service.MomentService
import com.prslc.zhiflow.data.service.QuestionService
import com.prslc.zhiflow.data.service.UserService
import com.prslc.zhiflow.ui.page.comment.CommentViewModel
import com.prslc.zhiflow.ui.page.content.CollectionViewModel
import com.prslc.zhiflow.ui.page.content.ContentViewModel
import com.prslc.zhiflow.ui.page.debug.DebugViewModel
import com.prslc.zhiflow.ui.page.feed.FeedViewModel
import com.prslc.zhiflow.ui.page.people.PeopleViewModel
import com.prslc.zhiflow.ui.page.people.moment.ActivitiesViewModel
import com.prslc.zhiflow.ui.page.people.moment.PostsViewModel
import com.prslc.zhiflow.ui.page.people.moment.UpvotesViewModel
import com.prslc.zhiflow.ui.page.profile.ProfileViewModel
import com.prslc.zhiflow.ui.page.question.QuestionViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // SharedPreferences
    single<SharedPreferences> {
        androidContext().getSharedPreferences("temp_auth_prefs", Context.MODE_PRIVATE)
    }

    // HttpClient
    single { Client.okHttpClient }

    // init UA
    single(createdAtStart = true) {
        HeaderProvider.init(androidContext())
    }

    // Feed
    singleOf(::FeedService)
    singleOf(::FeedRepository)
    viewModelOf(::FeedViewModel)

    // Collection
    singleOf(::CollectionService)
    singleOf(::CollectionRepository)
    viewModelOf(::CollectionViewModel)

    // Comment
    singleOf(::CommentService)
    singleOf(::CommentRepository)
    viewModelOf(::CommentViewModel)

    // User
    singleOf(::UserService)
    singleOf(::UserRepository)
    viewModelOf(::ProfileViewModel)

    // Content
    singleOf(::ContentService)
    singleOf(::ContentRepository)
    viewModelOf(::ContentViewModel)

    // Question
    singleOf(::QuestionService)
    singleOf(::QuestionRepository)
    viewModelOf(::QuestionViewModel)

    // Action
    singleOf(::ActionService)
    singleOf(::ActionRepository)

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
}
