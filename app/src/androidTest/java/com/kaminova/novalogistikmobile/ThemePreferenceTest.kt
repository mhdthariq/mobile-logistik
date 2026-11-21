package com.kaminova.novalogistikmobile

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.kaminova.novalogistikmobile.data.session.SessionManager
import com.kaminova.novalogistikmobile.ui.theme.NovaLogistikMobileTheme
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferenceTest {
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    fun phoneColorsApplyImmediatelyAndPersist() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settings = SessionManager(context)
        val previous = settings.usePhoneColors.value
        val appliedColors = LinkedBlockingQueue<Int>()
        try {
            settings.setUsePhoneColors(false)
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    activity.setContent {
                        val usePhoneColors = settings.usePhoneColors.collectAsStateWithLifecycle()
                        NovaLogistikMobileTheme(darkTheme = false, usePhoneColors = usePhoneColors.value) {
                            val primary = MaterialTheme.colorScheme.primary.toArgb()
                            SideEffect { appliedColors.offer(primary) }
                        }
                    }
                }

                assertEquals(Color(0xFF005746).toArgb(), appliedColors.poll(10, TimeUnit.SECONDS))
                scenario.onActivity { settings.setUsePhoneColors(true) }
                assertEquals(dynamicLightColorScheme(context).primary.toArgb(), appliedColors.poll(10, TimeUnit.SECONDS))
                assertEquals(true, SessionManager(context).usePhoneColors.value)
                scenario.onActivity { settings.setUsePhoneColors(false) }
                assertEquals(Color(0xFF005746).toArgb(), appliedColors.poll(10, TimeUnit.SECONDS))
            }
        } finally {
            settings.setUsePhoneColors(previous)
        }
    }
}
