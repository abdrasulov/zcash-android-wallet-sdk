package cash.z.ecc.android.sdk.demoapp.demos.getbalance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentGetBalanceBinding
import cash.z.ecc.android.sdk.demoapp.ext.requireApplicationContext
import cash.z.ecc.android.sdk.demoapp.util.SyncBlockchainBenchmarkTrace
import cash.z.ecc.android.sdk.demoapp.util.fromResources
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.tool.DerivationTool
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * Displays the available balance && total balance associated with the seed defined by the default config.
 * comments.
 */
@Suppress("TooManyFunctions")
class GetBalanceFragment : BaseDemoFragment<FragmentGetBalanceBinding>() {

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetBalanceBinding =
        FragmentGetBalanceBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reportTraceEvent(SyncBlockchainBenchmarkTrace.Event.BALANCE_SCREEN_START)
        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // We rather hide options menu actions while actively using the Synchronizer
        menu.setGroupVisible(R.id.main_menu_group, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        reportTraceEvent(SyncBlockchainBenchmarkTrace.Event.BALANCE_SCREEN_END)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val seedPhrase = sharedViewModel.seedPhrase.value
        val seed = Mnemonics.MnemonicCode(seedPhrase).toSeed()
        val network = ZcashNetwork.fromResources(requireApplicationContext())

        binding.shield.apply {
            setOnClickListener {
                lifecycleScope.launch {
                    sharedViewModel.synchronizerFlow.value?.shieldFunds(
                        DerivationTool.deriveUnifiedSpendingKey(
                            seed,
                            network,
                            Account.DEFAULT
                        )
                    )
                }
            }
        }

        monitorChanges()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun monitorChanges() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest { it.status }
                        .collect { onStatus(it) }
                }
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest { it.progress }
                        .collect { onProgress(it) }
                }
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest { it.processorInfo }
                        .collect { onProcessorInfoUpdated(it) }
                }
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest { it.saplingBalances }
                        .collect { onSaplingBalance(it) }
                }
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest { it.orchardBalances }
                        .collect { onOrchardBalance(it) }
                }
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest { it.transparentBalances }
                        .collect { onTransparentBalance(it) }
                }
            }
        }
    }

    private fun onOrchardBalance(
        orchardBalance: WalletBalance?
    ) {
        binding.orchardBalance.apply {
            text = orchardBalance.humanString()
        }
    }

    private fun onSaplingBalance(
        saplingBalance: WalletBalance?
    ) {
        binding.saplingBalance.apply {
            text = saplingBalance.humanString()
        }
    }

    private fun onTransparentBalance(
        transparentBalance: WalletBalance?
    ) {
        binding.transparentBalance.apply {
            text = transparentBalance.humanString()
        }

        binding.shield.apply {
            // TODO [#776]: Support variable fees
            // TODO [#776]: https://github.com/zcash/zcash-android-wallet-sdk/issues/776
            visibility = if ((transparentBalance?.available ?: Zatoshi(0)) > ZcashSdk.MINERS_FEE) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun onStatus(status: Synchronizer.Status) {
        Twig.debug { "Synchronizer status: $status" }
        // report benchmark event
        val traceEvents = when (status) {
            Synchronizer.Status.DOWNLOADING -> {
                listOf(
                    SyncBlockchainBenchmarkTrace.Event.BLOCKCHAIN_SYNC_START,
                    SyncBlockchainBenchmarkTrace.Event.DOWNLOAD_START
                )
            }
            Synchronizer.Status.VALIDATING -> {
                listOf(
                    SyncBlockchainBenchmarkTrace.Event.DOWNLOAD_END,
                    SyncBlockchainBenchmarkTrace.Event.VALIDATION_START
                )
            }
            Synchronizer.Status.SCANNING -> {
                listOf(
                    SyncBlockchainBenchmarkTrace.Event.VALIDATION_END,
                    SyncBlockchainBenchmarkTrace.Event.SCAN_START
                )
            }
            Synchronizer.Status.SYNCED -> {
                listOf(
                    SyncBlockchainBenchmarkTrace.Event.SCAN_END,
                    SyncBlockchainBenchmarkTrace.Event.BLOCKCHAIN_SYNC_END
                )
            }
            else -> null
        }
        traceEvents?.forEach { reportTraceEvent(it) }

        binding.textStatus.text = "Status: $status"
        sharedViewModel.synchronizerFlow.value?.let { synchronizer ->
            onOrchardBalance(synchronizer.orchardBalances.value)
            onSaplingBalance(synchronizer.saplingBalances.value)
            onTransparentBalance(synchronizer.transparentBalances.value)
        }
    }

    @Suppress("MagicNumber")
    private fun onProgress(i: Int) {
        if (i < 100) {
            binding.textStatus.text = "Downloading blocks...$i%"
        }
    }

    private fun onProcessorInfoUpdated(info: CompactBlockProcessor.ProcessorInfo) {
        if (info.isScanning) binding.textStatus.text = "Scanning blocks...${info.scanProgress}%"
    }
}

@Suppress("MagicNumber")
private fun WalletBalance?.humanString() = if (null == this) {
    "Calculating balance"
} else {
    """
                Pending balance: ${pending.convertZatoshiToZecString(12)}
                Available balance: ${available.convertZatoshiToZecString(12)}
                Total balance: ${total.convertZatoshiToZecString(12)}
    """.trimIndent()
}
