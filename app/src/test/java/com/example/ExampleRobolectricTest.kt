package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.aistudio.zenithstudio.rpxwtq.EffectStackManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Zenith Studio", appName)
  }

  @Test
  fun `verify downscale manager state defaults and updates`() {
    // Check initial state
    assertFalse(EffectStackManager.isDownscaleEnabled.value)
    assertEquals(2, EffectStackManager.downscaleFactor.value)
    assertFalse(EffectStackManager.downscaleBilinear.value)
    assertFalse(EffectStackManager.downscaleOnlyOnDrag.value)

    val initialCounter = EffectStackManager.changeCounter.value

    // Toggle on downscaling
    EffectStackManager.isDownscaleEnabled.value = true
    EffectStackManager.changeCounter.value++
    assertTrue(EffectStackManager.isDownscaleEnabled.value)
    assertEquals(initialCounter + 1, EffectStackManager.changeCounter.value)

    // Change downscale factor
    EffectStackManager.downscaleFactor.value = 4
    EffectStackManager.changeCounter.value++
    assertEquals(4, EffectStackManager.downscaleFactor.value)
    assertEquals(initialCounter + 2, EffectStackManager.changeCounter.value)

    // Reset for subsequent runs/app usage
    EffectStackManager.isDownscaleEnabled.value = false
    EffectStackManager.downscaleFactor.value = 2
  }
}
