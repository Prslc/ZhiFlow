package com.prslc.zhiflow.ui

import kotlinx.serialization.Serializable

@Serializable object MainContainer

@Serializable object HomeTab
@Serializable object DebugTab
@Serializable object ProfileTab

@Serializable object Settings
@Serializable data class AnswerDetail(val id: String)