package cash.z.ecc.android.sdk.demoapp.fixture

import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.demoapp.ui.screen.home.viewmodel.SynchronizerError
import cash.z.ecc.android.sdk.demoapp.ui.screen.home.viewmodel.WalletSnapshot
import cash.z.ecc.android.sdk.model.PercentDecimal
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi

@Suppress("MagicNumber")
object WalletSnapshotFixture {

    val STATUS = Synchronizer.Status.SYNCED
    val PROGRESS = PercentDecimal.ZERO_PERCENT
    val TRANSPARENT_BALANCE: WalletBalance = WalletBalance(Zatoshi(8), Zatoshi(1))
    val ORCHARD_BALANCE: WalletBalance = WalletBalance(Zatoshi(5), Zatoshi(2))
    val SAPLING_BALANCE: WalletBalance = WalletBalance(Zatoshi(4), Zatoshi(4))

    // Should fill in with non-empty values for better example values in tests and UI previews
    @Suppress("LongParameterList")
    fun new(
        status: Synchronizer.Status = STATUS,
        processorInfo: CompactBlockProcessor.ProcessorInfo = CompactBlockProcessor.ProcessorInfo(
            null,
            null,
            null,
            null,
            null
        ),
        orchardBalance: WalletBalance = ORCHARD_BALANCE,
        saplingBalance: WalletBalance = SAPLING_BALANCE,
        transparentBalance: WalletBalance = TRANSPARENT_BALANCE,
        pendingCount: Int = 0,
        progress: PercentDecimal = PROGRESS,
        synchronizerError: SynchronizerError? = null
    ) = WalletSnapshot(
        status,
        processorInfo,
        orchardBalance,
        saplingBalance,
        transparentBalance,
        pendingCount,
        progress,
        synchronizerError
    )
}
