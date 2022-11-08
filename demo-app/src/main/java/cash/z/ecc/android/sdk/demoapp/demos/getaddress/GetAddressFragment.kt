package cash.z.ecc.android.sdk.demoapp.demos.getaddress

import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentGetAddressBinding
import cash.z.ecc.android.sdk.demoapp.ext.requireApplicationContext
import cash.z.ecc.android.sdk.demoapp.util.fromResources
import cash.z.ecc.android.sdk.model.LightWalletEndpoint
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.model.defaultForNetwork
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.UnifiedFullViewingKey
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Displays the address associated with the seed defined by the default config. To modify the seed
 * that is used, update the `DemoConfig.seedWords` value.
 */
class GetAddressFragment : BaseDemoFragment<FragmentGetAddressBinding>() {

    private lateinit var synchronizer: Synchronizer
    private lateinit var viewingKey: UnifiedFullViewingKey
    private lateinit var seed: ByteArray

    /**
     * Initialize the required values that would normally live outside the demo but are repeated
     * here for completeness so that each demo file can serve as a standalone example.
     */
    private fun setup() {
        // defaults to the value of `DemoConfig.seedWords` but can also be set by the user
        val seedPhrase = sharedViewModel.seedPhrase.value

        // Use a BIP-39 library to convert a seed phrase into a byte array. Most wallets already
        // have the seed stored
        seed = Mnemonics.MnemonicCode(seedPhrase).toSeed()

        // converting seed into viewingKey
        viewingKey = runBlocking {
            DerivationTool.deriveUnifiedFullViewingKeys(
                seed,
                ZcashNetwork.fromResources(requireApplicationContext())
            ).first()
        }

        val network = ZcashNetwork.fromResources(requireApplicationContext())
        synchronizer = Synchronizer.newBlocking(
            requireApplicationContext(),
            network,
            lightWalletEndpoint = LightWalletEndpoint.defaultForNetwork(network),
            seed = seed,
            birthday = network.saplingActivationHeight
        )
    }

    private fun displayAddress() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            binding.unifiedAddress.apply {
                val uaddress = synchronizer.getUnifiedAddress()
                text = uaddress
                setOnClickListener { copyToClipboard(uaddress) }
            }
            binding.saplingAddress.apply {
                val sapling = synchronizer.getSaplingAddress()
                text = sapling
                setOnClickListener { copyToClipboard(sapling) }
            }
            binding.transparentAddress.apply {
                val transparent = synchronizer.getTransparentAddress()
                text = transparent
                setOnClickListener { copyToClipboard(transparent) }
            }
        }
    }

    //
    // Android Lifecycle overrides
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setup()
    }

    override fun onResume() {
        super.onResume()
        displayAddress()
    }

    //
    // Base Fragment overrides
    //

    override fun onActionButtonClicked() {
        viewLifecycleOwner.lifecycleScope.launch {
            copyToClipboard(
                DerivationTool.deriveUnifiedAddress(
                    viewingKey.encoding,
                    ZcashNetwork.fromResources(requireApplicationContext())
                ),
                "Unified address copied to clipboard!"
            )
        }
    }

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetAddressBinding =
        FragmentGetAddressBinding.inflate(layoutInflater)
}
