package io.github.zenhelix.ktlint.rules

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class LineLengthSettingsTest {

    @Nested
    inner class `default values` {
        @Test
        fun `should use 160 as default max line length`() {
            val settings = LineLengthSettings()
            assertThat(settings.hard).isEqualTo(160)
            assertThat(settings.collapse).isEqualTo(130)
            assertThat(settings.standard).isEqualTo(120)
            assertThat(settings.collapseFunctional).isEqualTo(120)
        }
    }

    @Nested
    inner class `proportional scaling` {
        @Test
        fun `should scale proportionally for max line length 120`() {
            val settings = LineLengthSettings(120)
            assertThat(settings.hard).isEqualTo(120)
            assertThat(settings.collapse).isEqualTo(98)
            assertThat(settings.standard).isEqualTo(90)
            assertThat(settings.collapseFunctional).isEqualTo(90)
        }

        @Test
        fun `should scale proportionally for max line length 140`() {
            val settings = LineLengthSettings(140)
            assertThat(settings.hard).isEqualTo(140)
            assertThat(settings.collapse).isEqualTo(114)
            assertThat(settings.standard).isEqualTo(105)
            assertThat(settings.collapseFunctional).isEqualTo(105)
        }

        @Test
        fun `should scale proportionally for max line length 200`() {
            val settings = LineLengthSettings(200)
            assertThat(settings.hard).isEqualTo(200)
            assertThat(settings.collapse).isEqualTo(162)
            assertThat(settings.standard).isEqualTo(150)
            assertThat(settings.collapseFunctional).isEqualTo(150)
        }
    }

    @Nested
    inner class `edge cases` {
        @Test
        fun `should handle max value (off) by using default`() {
            val settings = LineLengthSettings(Int.MAX_VALUE)
            assertThat(settings.hard).isEqualTo(Int.MAX_VALUE)
        }
    }
}
