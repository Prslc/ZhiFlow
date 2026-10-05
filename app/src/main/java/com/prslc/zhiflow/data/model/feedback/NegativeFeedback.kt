package com.prslc.zhiflow.data.model.feedback

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of `GET /negative-feedback/panel`, a server-driven list of buttons. */
@Immutable
@Serializable
data class NegativeFeedbackPanel(
    val data: NegativeFeedbackPanelData = NegativeFeedbackPanelData(),
)

@Immutable
@Serializable
data class NegativeFeedbackPanelData(
    val items: List<NegativeFeedbackItem> = emptyList(),
    val style: String = "",
)

@Immutable
@Serializable
data class NegativeFeedbackItem(
    @SerialName("raw_button") val rawButton: RawButton = RawButton(),
)

@Immutable
@Serializable
data class RawButton(
    val style: String? = null,
    val icon: ButtonIcon? = null,
    @SerialName("right_icon") val rightIcon: ButtonIcon? = null,
    val text: ButtonText = ButtonText(),
    val action: ButtonAction = ButtonAction(),
)

@Immutable
@Serializable
data class ButtonIcon(
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("night_image_url") val nightImageUrl: String? = null,
    val width: Int = 0,
    val height: Int = 0,
)

@Immutable
@Serializable
data class ButtonText(
    @SerialName("panel_text") val panelText: String = "",
    @SerialName("toast_text") val toastText: String? = null,
    @SerialName("max_line") val maxLine: Int = 1,
)

@Immutable
@Serializable
data class ButtonAction(
    @SerialName("backend_url") val backendUrl: String? = null,
    @SerialName("intent_url") val intentUrl: String? = null,
    val method: String = "GET",
    val module: ButtonModule? = null,
)

@Immutable
@Serializable
data class ButtonModule(
    @SerialName("module_id") val moduleId: String = "",
)
