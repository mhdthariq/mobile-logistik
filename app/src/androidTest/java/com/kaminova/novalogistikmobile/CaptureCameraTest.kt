package com.kaminova.novalogistikmobile

import android.Manifest
import android.graphics.Rect
import android.os.SystemClock
import com.kaminova.novalogistikmobile.data.api.UserSummary
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.kaminova.novalogistikmobile.data.session.CaptureManager
import com.kaminova.novalogistikmobile.data.session.CaptureRequest
import com.kaminova.novalogistikmobile.data.session.SessionManager
import com.kaminova.novalogistikmobile.ui.screens.capture.CaptureScreen
import com.kaminova.novalogistikmobile.ui.screens.capture.CaptureViewModel
import com.kaminova.novalogistikmobile.ui.theme.NovaLogistikMobileTheme
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 28)
class CaptureCameraTest {
    @Test
    fun changesCameraAndCapturesPhoto() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        for (permission in listOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            automation.grantRuntimePermission(instrumentation.targetContext.packageName, permission)
        }

        fun find(node: AccessibilityNodeInfo?, label: String, enabledOnly: Boolean): AccessibilityNodeInfo? {
            if (node == null) return null
            if ((!enabledOnly || node.isEnabled) &&
                (node.contentDescription?.toString() == label || node.text?.toString() == label)) return node
            for (index in 0 until node.childCount) {
                find(node.getChild(index), label, enabledOnly)?.let { return it }
            }
            return null
        }

        fun await(label: String, enabledOnly: Boolean = true): AccessibilityNodeInfo {
            val deadline = SystemClock.uptimeMillis() + 15000
            while (SystemClock.uptimeMillis() < deadline) {
                find(automation.rootInActiveWindow, label, enabledOnly)?.let { return it }
                SystemClock.sleep(100)
            }
            error("Control never became available: $label")
        }

        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                CaptureManager.requestCapture(CaptureRequest(
                    title = "Foto Aktivitas",
                    requireGps = false,
                    driver = UserSummary(1, "Sopir Tes", "0821-8080-0104")
                ))
                val model = CaptureViewModel(SessionManager(activity), activity)
                activity.setContent { NovaLogistikMobileTheme { CaptureScreen(model, onNavigateBack = {}) } }
            }
            try {
                val flash = await("Nyalakan lampu", enabledOnly = false)
                val shutter = await("Ambil foto")
                val switch = await("Gunakan kamera depan")
                fun bounds(node: AccessibilityNodeInfo) = Rect().apply { node.getBoundsInScreen(this) }
                assertTrue(bounds(flash).centerX() < bounds(shutter).centerX())
                assertTrue(bounds(switch).centerX() > bounds(shutter).centerX())
                assertTrue(kotlin.math.abs(bounds(switch).centerY() - bounds(shutter).centerY()) < 4)
                for (zone in listOf("WIB", "WITA", "WIT")) {
                    assertNull(find(automation.rootInActiveWindow, zone, enabledOnly = false))
                }
                assertTrue(await("Gunakan kamera depan").performAction(AccessibilityNodeInfo.ACTION_CLICK))
                await("Gunakan kamera belakang")
                assertTrue(await("Ambil foto").performAction(AccessibilityNodeInfo.ACTION_CLICK))
                await("Gunakan Foto")
            } finally {
                scenario.onActivity { CaptureManager.completeCapture(null) }
            }
        }
    }
}
