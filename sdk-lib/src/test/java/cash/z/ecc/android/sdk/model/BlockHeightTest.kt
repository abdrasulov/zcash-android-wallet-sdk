package cash.z.ecc.android.sdk.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BlockHeightTest {
    @Test
    fun new_mainnet_fails_below_sapling_activation_height() {
        assertFailsWith(IllegalArgumentException::class) {
            BlockHeight.new(
                ZcashNetwork.Mainnet,
                ZcashNetwork.Mainnet.saplingActivationHeight.value - 1
            )
        }
    }

    @Test
    fun new_mainnet_succeeds_at_sapling_activation_height() {
        BlockHeight.new(ZcashNetwork.Mainnet, ZcashNetwork.Mainnet.saplingActivationHeight.value)
    }

    @Test
    fun new_mainnet_succeeds_above_sapling_activation_height() {
        BlockHeight.new(ZcashNetwork.Mainnet, ZcashNetwork.Mainnet.saplingActivationHeight.value + 10_000)
    }

    @Test
    fun new_mainnet_succeeds_at_max_value() {
        BlockHeight.new(ZcashNetwork.Mainnet, UInt.MAX_VALUE.toLong())
    }

    @Test
    fun new_fails_above_max_value() {
        assertFailsWith(IllegalArgumentException::class) {
            BlockHeight.new(ZcashNetwork.Mainnet, UInt.MAX_VALUE.toLong() + 1)
        }
    }

    @Test
    fun addition_of_blockheight_succeeds() {
        val one = BlockHeight.new(ZcashNetwork.Mainnet, ZcashNetwork.Mainnet.saplingActivationHeight.value)
        val two = BlockHeight.new(ZcashNetwork.Mainnet, ZcashNetwork.Mainnet.saplingActivationHeight.value + 123)

        assertEquals(838523L, (one + two).value)
    }

    @Test
    fun addition_of_int_succeeds() {
        assertEquals(419323L, (ZcashNetwork.Mainnet.saplingActivationHeight + 123).value)
    }

    @Test
    fun addition_of_long_succeeds() {
        assertEquals(419323L, (ZcashNetwork.Mainnet.saplingActivationHeight + 123L).value)
    }

    @Test
    fun subtraction_of_int_fails() {
        assertFailsWith<IllegalArgumentException> {
            ZcashNetwork.Mainnet.saplingActivationHeight + -1
        }
    }

    @Test
    fun subtraction_of_long_fails() {
        assertFailsWith<IllegalArgumentException> {
            ZcashNetwork.Mainnet.saplingActivationHeight + -1L
        }
    }
}
