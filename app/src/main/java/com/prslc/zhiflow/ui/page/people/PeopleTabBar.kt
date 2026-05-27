package com.prslc.zhiflow.ui.page.people

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.prslc.zhiflow.R
import kotlin.math.absoluteValue

private val INDICATOR_TEXT_WIDTH = 30.dp
private val INDICATOR_HEIGHT = 3.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PeopleTabBar(
    pagerState: PagerState,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        stringResource(R.string.people_tab_post),
        stringResource(R.string.people_tab_activity),
        stringResource(R.string.people_tab_upvoted)
    )

    Surface(modifier = modifier.fillMaxWidth()) {
        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = {
                val customIndicatorModifier = remember(pagerState) {
                    Modifier.tabIndicatorLayout { measurable, constraints, tabPositions ->
                        if (tabPositions.isEmpty()) {
                            return@tabIndicatorLayout layout(0, 0) {}
                        }

                        val currentPage = pagerState.currentPage
                        val fraction = pagerState.currentPageOffsetFraction

                        val safeCurrentPage = minOf(tabPositions.lastIndex, currentPage)
                        val currentTab = tabPositions[safeCurrentPage]
                        val targetPage =
                            if (fraction < 0) safeCurrentPage - 1 else safeCurrentPage + 1
                        val targetTab = tabPositions.getOrNull(targetPage) ?: currentTab

                        val desiredWidthPx = INDICATOR_TEXT_WIDTH.roundToPx()

                        val currentLeftCenter =
                            currentTab.left + (currentTab.width - INDICATOR_TEXT_WIDTH) / 2
                        val targetLeftCenter =
                            targetTab.left + (targetTab.width - INDICATOR_TEXT_WIDTH) / 2

                        val indicatorOffset =
                            lerp(currentLeftCenter, targetLeftCenter, fraction.absoluteValue)

                        val placeable = measurable.measure(
                            constraints.copy(
                                minWidth = desiredWidthPx,
                                maxWidth = desiredWidthPx
                            )
                        )

                        layout(constraints.maxWidth, placeable.height) {
                            placeable.placeRelative(indicatorOffset.roundToPx(), 0)
                        }
                    }
                }

                TabRowDefaults.SecondaryIndicator(
                    modifier = customIndicatorModifier,
                    color = MaterialTheme.colorScheme.primary,
                    height = INDICATOR_HEIGHT
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = pagerState.currentPage == index
                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    }
}
