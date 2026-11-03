package com.edigitalhub.freemem

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.util.concurrent.Callable

class FreeMemServiceTest : BasePlatformTestCase() {

    // The scheduled check and the LowMemoryWatcher both reclaim from pooled threads
    fun testAutomaticReclaimFromBackgroundThread() {
        FreeMemSettingsState.getInstance().state.dropPsiCaches = true

        val future = ApplicationManager.getApplication().executeOnPooledThread(Callable {
            FreeMemService.getInstance().performReclaim(isManual = false, targetProject = null)
        })

        val stats = PlatformTestUtil.waitForFuture(future, 60_000)
        assertTrue(stats.afterMB > 0)
    }
}
