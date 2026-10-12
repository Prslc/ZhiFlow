package com.prslc.zhiflow.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

/** The app the profile is generated for. */
private const val APP_PACKAGE = "com.prslc.zhiflow"

/**
 * The startup profile: what the app compiles while its first screen is drawn.
 *
 * The rule starts the app on a device and records what it actually runs, so the profile is the
 * startup path itself rather than a guess at it -- the Koin graph, the first composition, the
 * session the interceptor waits for, the first page of the feed.
 *
 * Nothing is touched beyond starting it. A matcher would have to be written against text the
 * server sends, which is neither this app's nor stable, and a profile is only worth what a cold
 * start does anyway.
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
    }
}
