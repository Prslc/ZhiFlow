package com.prslc.zhiflow.baselineprofile

import android.os.SystemClock
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

/** The app the profile is generated for. */
private const val APP_PACKAGE = "com.prslc.zhiflow"

/** What the app exposes as resource ids. */
private const val FEED_LIST = "feed_list"
private const val CONTENT_PAGE = "content_page"
private const val CONTENT_BODY = "content_body"
private const val PROFILE_TAB = "tab_profile"

/** The journey's shape. */
private const val FEED_SWIPES = 3
private const val CONTENT_SWIPES = 3
private const val ROWS_TO_TRY = 3

/** How long each step is given before the journey gives up on it. */
private const val FEED_TIMEOUT_MS = 15_000L
private const val CONTENT_TIMEOUT_MS = 15_000L
private const val LANDING_TIMEOUT_MS = 3_000L
private const val TAB_TIMEOUT_MS = 5_000L
private const val POLL_MS = 100L

/**
 * The startup journey: what the app compiles from a cold start, through the feed and a content
 * page, to another tab.
 *
 * The runner force-stops the app before every iteration, so every run starts cold and records what
 * the journey actually executes: the Koin graph, the first composition, the session the
 * interceptor waits for, the first page of the feed, three swipes of it, a content page's parse
 * and draw while it is swiped, and the profile tab drawing itself for the first time.
 *
 * The journey is addressed by the tags the app exposes as resource ids, not by text: what the
 * server sends is neither this app's nor stable, and a matcher written against it fails by saying
 * the journey never happened.
 *
 * Run by hand, against a connected device: `./gradlew :app:generateBaselineProfile`. The device
 * has to be API 33 or higher; it does not have to be rooted.
 */
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collect(packageName = APP_PACKAGE) {
        startActivityAndWait()
        awaitFeed()

        repeat(FEED_SWIPES) {
            feedList().fling(Direction.DOWN)
            device.waitForIdle()
        }

        openContentPage()
        awaitContentBody()
        repeat(CONTENT_SWIPES) {
            contentBody().fling(Direction.DOWN)
            device.waitForIdle()
        }
        leaveContentPage()

        val profileTab = checkNotNull(device.findObject(By.res(PROFILE_TAB))) {
            "The profile tab is not on screen."
        }
        profileTab.click()
        check(device.wait(Until.hasObject(By.res(PROFILE_TAB).selected(true)), TAB_TIMEOUT_MS)) {
            "The tab was tapped but never became the selected one."
        }
        device.waitForIdle()
    }

    /**
     * Opens the page of the first feed row that has one. The feed mixes answers, articles and pins
     * with cards that lead somewhere else, so clicking one row would make the journey depend on
     * which card the server sent first.
     */
    private fun MacrobenchmarkScope.openContentPage() {
        repeat(ROWS_TO_TRY) { index ->
            val row = feedRows().getOrNull(index) ?: return@repeat
            val bounds = row.visibleBounds
            if (bounds.height() <= 0) return@repeat

            // A quarter of the way down, above the thumbnails: those are clickable in their own
            // right and would open the lightbox instead of the page.
            device.click(bounds.centerX(), bounds.top + bounds.height() / 4)

            if (device.wait(Until.hasObject(By.res(CONTENT_PAGE)), LANDING_TIMEOUT_MS)) return
            if (!device.hasObject(By.res(FEED_LIST))) {
                device.pressBack()
                check(device.wait(Until.hasObject(By.res(FEED_LIST)), LANDING_TIMEOUT_MS)) {
                    "Back did not return to the feed."
                }
                // The row sits under a page that is still sliding out of the way for a moment.
                device.waitForIdle()
            }
        }
        error("No row opened a content page.")
    }

    /** The body arrives after the page does; until it does, there is nothing on it to swipe. */
    private fun MacrobenchmarkScope.awaitContentBody() {
        check(device.wait(Until.hasObject(By.res(CONTENT_BODY)), CONTENT_TIMEOUT_MS)) {
            "The page never drew its body."
        }
        device.waitForIdle()
    }

    /**
     * Back to the feed, and past the page's exit: the tab bar sits under the page while it slides
     * away, so tapping before it is gone would land on the page instead.
     */
    private fun MacrobenchmarkScope.leaveContentPage() {
        device.pressBack()
        check(device.wait(Until.gone(By.res(CONTENT_PAGE)), LANDING_TIMEOUT_MS)) {
            "Back did not leave the content page."
        }
        check(device.wait(Until.hasObject(By.res(FEED_LIST)), LANDING_TIMEOUT_MS)) {
            "The feed is not back on screen."
        }
        device.waitForIdle()
    }

    /**
     * Waits until the feed's first page is in the list. The list itself is drawn before it holds
     * anything -- it is where the empty state and the error state sit -- so swiping as soon as the
     * screen settles would be swiping a list with nothing in it.
     */
    private fun MacrobenchmarkScope.awaitFeed() {
        val deadline = SystemClock.uptimeMillis() + FEED_TIMEOUT_MS
        while (SystemClock.uptimeMillis() < deadline) {
            val list = device.findObject(By.res(FEED_LIST))
            if (list != null && list.hasObject(By.longClickable(true))) return
            Thread.sleep(POLL_MS)
        }
        error("The feed never arrived, so there was nothing to swipe.")
    }

    /** Looked up again per swipe: a fling can leave the reference it was handed stale. */
    private fun MacrobenchmarkScope.feedList() =
        checkNotNull(device.findObject(By.res(FEED_LIST))) { "The feed's list is not on screen." }

    /** A row is the one part of the list that answers a long press; a thumbnail does not. */
    private fun MacrobenchmarkScope.feedRows() = feedList().findObjects(By.longClickable(true))

    private fun MacrobenchmarkScope.contentBody() =
        checkNotNull(device.findObject(By.res(CONTENT_BODY))) { "The page's body is not on screen." }
}
