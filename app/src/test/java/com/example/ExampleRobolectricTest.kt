package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Crimson Sector", appName)
  }

  @Test
  fun `template serialization preserves pistol attacks and attack count`() {
    val baseTpl = com.example.engine.DefaultCatalog.defaultTemplates.first { it.id == "hellblasters" }
    val customTpl = baseTpl.copy(attacksPerModel = 4, pistolAttacks = 3)
    val json = com.example.engine.DefaultCatalog.serializeTemplates(mapOf(customTpl.id to customTpl))
    val restored = com.example.engine.DefaultCatalog.deserializeTemplates(json)["hellblasters"]
    org.junit.Assert.assertNotNull(restored)
    assertEquals(4, restored!!.attacksPerModel)
    assertEquals(3, restored.pistolAttacks)
  }
}
