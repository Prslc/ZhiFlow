package com.prslc.zhiflow.ui.page.people

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.utils.formatCount
import com.prslc.zhiflow.data.model.ZhihuUser
import com.prslc.zhiflow.ui.component.common.ErrorView
import org.koin.androidx.compose.koinViewModel

@Composable
fun PeopleScreen(
    urlToken: String,
    onBack: () -> Unit,
    viewModel: PeopleViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState

    LaunchedEffect(urlToken) {
        viewModel.loadPeople(urlToken)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.user != null -> {
                    PeopleDetailContent(
                        user = uiState.user,
                        onBack = onBack,
                        bottomPadding = innerPadding.calculateBottomPadding()
                    )
                }

                uiState.isLoading -> {
                    Box(Modifier.padding(innerPadding).fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }

                uiState.error != null -> {
                    Box(Modifier.padding(innerPadding).fillMaxSize()) {
                        ErrorView(
                            message = uiState.error.uiMessage,
                            onRetry = { viewModel.loadPeople(urlToken) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            if (uiState.user == null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.general_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PeopleDetailContent(
    user: ZhihuUser,
    onBack: () -> Unit,
    bottomPadding: Dp
) {
    val scrollState = rememberLazyListState()

    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                // cover + backButton
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    AsyncImage(
                        model = user.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.general_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .offset(y = (-30).dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Surface(
                            modifier = Modifier.size(84.dp),
                            shape = CircleShape,
                            border = BorderStroke(3.dp, MaterialTheme.colorScheme.background)
                        ) {
                            AsyncImage(
                                model = user.avatar,
                                contentDescription = stringResource(R.string.content_desc_avatar),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatItem(stringResource(R.string.people_stat_voteup), user.voteupCount)
                            StatItem(
                                stringResource(R.string.people_stat_followers),
                                user.followerCount
                            )
                            StatItem(
                                stringResource(R.string.people_stat_following),
                                user.followingCount
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = user.name ?: stringResource(R.string.profile_default_username),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user.headline ?: stringResource(R.string.profile_default_headline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // action button
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {},
                            modifier = Modifier.weight(1.3f),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.people_action_follow))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        FilledTonalButton(
                            onClick = {},
                            modifier = Modifier.weight(0.7f),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.MailOutline, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.people_action_message))
                        }
                    }
                }
            }
        }

        // Tab
        stickyHeader {
            Surface(modifier = Modifier.fillMaxWidth()) {
                Column {
                    HorizontalDivider(thickness = 0.5.dp)
                    Row(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.people_tab_work),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Text(
                            text = stringResource(R.string.people_tab_dynamic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        // TODO: list
    }
}

@Composable
fun StatItem(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatCount(count),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
