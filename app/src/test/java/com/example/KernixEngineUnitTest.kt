package com.example

import com.example.data.model.CustomLanguage
import com.example.data.model.LinuxProfile
import com.example.domain.ConfigGenerators
import com.example.domain.KernixLanguageEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KernixEngineUnitTest {

    @Test
    fun `executes program written with custom user keywords`() {
        val customLang = CustomLanguage(
            name = "FacilScript",
            fileExtension = ".fc",
            commentPrefix = "--",
            kwVar = "crear",
            kwPrint = "decir",
            kwFunc = "accion",
            kwIf = "si",
            kwElse = "sino",
            kwLoop = "repetir",
            kwReturn = "dar",
            kwTrue = "verdad",
            kwFalse = "falso"
        )
        val source = """
            -- Test custom language keywords
            accion doble(n) {
                dar n * 2
            }
            crear total = 0
            repetir i in 1..3 {
                total = total + doble(i)
            }
            si total == 12 {
                decir "OK: " + total
            } sino {
                decir "FAIL"
            }
        """.trimIndent()

        val result = KernixLanguageEngine.execute(source, customLang, LinuxProfile())
        assertNull(result.errorMessage)
        assertEquals(listOf("OK: 12"), result.outputLines)
    }

    @Test
    fun `generates kernel config and hyprland dotfiles accurately`() {
        val profile = LinuxProfile(
            name = "MyRig",
            architecture = "x86_64",
            scheduler = "BORE",
            preemption = "PREEMPT_RT",
            desktopEnv = "Hyprland",
            windowGaps = 12,
            cornerRadius = 16
        )
        val kconfig = ConfigGenerators.generateKernelConfig(profile)
        assertTrue(kconfig.contains("CONFIG_SCHED_BORE=y"))
        assertTrue(kconfig.contains("CONFIG_PREEMPT_RT=y"))

        val hypr = ConfigGenerators.generateDesktopConfig(profile)
        assertTrue(hypr.contains("gaps_out = 12"))
        assertTrue(hypr.contains("rounding = 16"))
    }
}
