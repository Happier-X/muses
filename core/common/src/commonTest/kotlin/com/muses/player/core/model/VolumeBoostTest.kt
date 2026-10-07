package com.muses.player.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class VolumeBoostTest {
    @Test fun `增益档位换算为毫贝`() {
        assertEquals(0, volumeBoostMillibels(0))
        assertEquals(600, volumeBoostMillibels(DEFAULT_VOLUME_BOOST_DB))
    }

    @Test fun `超出档位上限按上限钳制`() {
        assertEquals(MAX_VOLUME_BOOST_DB * 100, volumeBoostMillibels(99))
        assertEquals(0, volumeBoostMillibels(-3))
    }
}
