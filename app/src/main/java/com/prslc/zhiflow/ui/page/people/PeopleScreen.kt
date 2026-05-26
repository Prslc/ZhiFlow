package com.prslc.zhiflow.ui.page.people

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.page.people.moment.AutoLoadMoreEffect
import com.prslc.zhiflow.ui.page.people.moment.MomentViewModel
import com.prslc.zhiflow.ui.page.people.moment.momentsContent
import org.koin.androidx.compose.koinViewModel

@Composable
fun PeopleScreen(
    urlToken: String,
    onBack: () -> Unit,
    viewModel: PeopleViewModel = koinViewModel(),
    momentViewModel: MomentViewModel = koinViewModel()
) {
    val uiState = viewModel.uiState
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val listState = momentViewModel.listState
    val density = LocalDensity.current

    val statusBarHeightPx = WindowInsets.statusBars.getTop(density)
    val topBarHeightDp = 48.dp
    val topBarHeightPx = with(density) { topBarHeightDp.toPx() }
    val totalTopHeightPx = statusBarHeightPx + topBarHeightPx

    val topBarAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else {
                val offset = listState.firstVisibleItemScrollOffset
                (offset / 400f).coerceIn(0f, 1f)
            }
        }
    }

    val isTabsPinned by remember {
        derivedStateOf {
            val tabsItem = listState.layoutInfo.visibleItemsInfo.find { it.index == 1 }
            if (tabsItem != null) {
                tabsItem.offset <= totalTopHeightPx
            } else {
                listState.firstVisibleItemIndex > 1
            }
        }
    }

    LaunchedEffect(urlToken) {
        viewModel.loadPeople(urlToken)
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && momentViewModel.uiState.moments.isEmpty()) {
            momentViewModel.loadMoment(urlToken)
        }
    }

    AutoLoadMoreEffect(momentViewModel, selectedTab)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->

        Box(modifier = Modifier.fillMaxSize()) {

            when {
                uiState.user != null -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        item(key = "header") {
                            PeopleHeader(user = uiState.user)
                        }

                        item(key = "tabs") {
                            PeopleTabBar(
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it },
                                modifier = Modifier.alpha(if (isTabsPinned) 0f else 1f)
                            )
                        }

                        when (selectedTab) {
                            0 -> { /* Undeveloped */ }
                            1 -> momentsContent(
                                urlToken = urlToken,
                                state = momentViewModel.uiState,
                                viewModel = momentViewModel
                            )
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {

                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = topBarAlpha),
                            shadowElevation = if (topBarAlpha > 0.9f && !isTabsPinned) 2.dp else 0.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .height(topBarHeightDp)
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.align(Alignment.CenterStart)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.general_back),
                                        tint = if (topBarAlpha > 0.5f) MaterialTheme.colorScheme.onSurface else Color.White
                                    )
                                }

                                if (topBarAlpha > 0.8f) {
                                    Text(
                                        text = uiState.user.name ?: "",
                                        modifier = Modifier.align(Alignment.Center),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { /* More */ }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More",
                                            tint = if (topBarAlpha > 0.5f) MaterialTheme.colorScheme.onSurface else Color.White
                                        )
                                    }
                                }
                            }
                        }

                        if (isTabsPinned) {
                            Surface(shadowElevation = 2.dp) {
                                PeopleTabBar(
                                    selectedTab = selectedTab, onTabSelected = { selectedTab = it })
                            }
                        }
                    }
                }

                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }

                uiState.error != null -> {
                    Box(Modifier.fillMaxSize()) {
                        ErrorView(
                            message = uiState.error.uiMessage,
                            onRetry = { viewModel.loadPeople(urlToken) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    IconButton(
                        onClick = onBack, modifier = Modifier
                            .statusBarsPadding()
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.general_back)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PeopleTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider(thickness = 0.5.dp)
            Row(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.people_tab_work),
                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onTabSelected(0) }
                )
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = stringResource(R.string.people_tab_dynamic),
                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onTabSelected(1) }
                )
            }
        }
    }
}
