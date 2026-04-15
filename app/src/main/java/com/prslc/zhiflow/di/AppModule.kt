package com.prslc.zhiflow.di

import com.prslc.zhiflow.core.network.Client
import com.prslc.zhiflow.data.repository.CollectionRepository
import com.prslc.zhiflow.data.repository.CommentRepository
import com.prslc.zhiflow.data.repository.FeedRepository
import com.prslc.zhiflow.data.repository.UserRepository
import com.prslc.zhiflow.data.service.CollectionService
import com.prslc.zhiflow.data.service.CommentService
import com.prslc.zhiflow.data.service.FeedService
import com.prslc.zhiflow.data.service.UserService
import com.prslc.zhiflow.ui.page.comment.CommentViewModel
import com.prslc.zhiflow.ui.page.content.CollectionViewModel
import com.prslc.zhiflow.ui.page.feed.FeedViewModel
import com.prslc.zhiflow.ui.page.profile.ProfileViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // HttpClient
    single { Client.client }

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
}