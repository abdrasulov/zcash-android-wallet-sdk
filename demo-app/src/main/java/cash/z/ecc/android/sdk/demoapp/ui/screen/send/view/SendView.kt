package cash.z.ecc.android.sdk.demoapp.ui.screen.send.view

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.demoapp.ui.common.MINIMAL_WEIGHT
import cash.z.ecc.android.sdk.demoapp.ui.screen.home.viewmodel.WalletSnapshot
import cash.z.ecc.android.sdk.demoapp.util.fromResources
import cash.z.ecc.android.sdk.fixture.WalletFixture
import cash.z.ecc.android.sdk.model.Memo
import cash.z.ecc.android.sdk.model.MonetarySeparators
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.model.ZecSend
import cash.z.ecc.android.sdk.model.ZecSendExt
import cash.z.ecc.android.sdk.model.ZecString
import cash.z.ecc.android.sdk.model.ZecStringExt
import cash.z.ecc.android.sdk.model.toZecString

// @Preview
// @Composable
// fun ComposablePreview() {
//     MaterialTheme {
//         Send()
//     }
// }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Send(
    walletSnapshot: WalletSnapshot,
    onSend: (ZecSend) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(topBar = {
        SendTopAppBar(onBack)
    }) { paddingValues ->
        SendMainContent(
            paddingValues = paddingValues,
            walletSnapshot = walletSnapshot,
            onSend = onSend
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SendTopAppBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.menu_send)) },
        navigationIcon = {
            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod")
private fun SendMainContent(
    paddingValues: PaddingValues,
    walletSnapshot: WalletSnapshot,
    onSend: (ZecSend) -> Unit
) {
    val context = LocalContext.current
    val monetarySeparators = MonetarySeparators.current()
    val allowedCharacters = ZecString.allowedCharacters(monetarySeparators)

    var amountZecString by rememberSaveable {
        mutableStateOf("")
    }
    var recipientAddressString by rememberSaveable {
        mutableStateOf("")
    }
    var memoString by rememberSaveable { mutableStateOf("") }

    var validation by rememberSaveable {
        mutableStateOf<Set<ZecSendExt.ZecSendValidation.Invalid.ValidationError>>(emptySet())
    }

    Column(
        Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(top = paddingValues.calculateTopPadding())
    ) {
        Text(text = stringResource(id = R.string.send_available_balance))
        Row(Modifier.fillMaxWidth()) {
            Text(text = walletSnapshot.saplingBalance.available.toZecString())
        }

        TextField(
            value = amountZecString,
            onValueChange = { newValue ->
                if (!ZecStringExt.filterContinuous(context, monetarySeparators, newValue)) {
                    return@TextField
                }
                amountZecString = newValue.filter { allowedCharacters.contains(it) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(stringResource(id = R.string.send_amount)) }
        )

        Spacer(Modifier.size(8.dp))

        TextField(
            value = recipientAddressString,
            onValueChange = { recipientAddressString = it },
            label = { Text(stringResource(id = R.string.send_to_address)) }
        )

        val zcashNetwork = ZcashNetwork.fromResources(context)
        Column(
            Modifier
                .fillMaxWidth()
        ) {
            // Alice's addresses
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Button({ recipientAddressString = WalletFixture.Alice.getAddresses(zcashNetwork).unified }) {
                    Text(text = stringResource(id = R.string.send_alyssa_unified))
                }

                Spacer(Modifier.size(8.dp))

                Button({ recipientAddressString = WalletFixture.Alice.getAddresses(zcashNetwork).sapling }) {
                    Text(text = stringResource(id = R.string.send_alyssa_sapling))
                }

                Spacer(Modifier.size(8.dp))

                Button({ recipientAddressString = WalletFixture.Alice.getAddresses(zcashNetwork).transparent }) {
                    Text(text = stringResource(id = R.string.send_alyssa_transparent))
                }
            }
            // Bob's addresses
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Button({ recipientAddressString = WalletFixture.Ben.getAddresses(zcashNetwork).unified }) {
                    Text(text = stringResource(id = R.string.send_ben_unified))
                }

                Spacer(Modifier.size(8.dp))

                Button({ recipientAddressString = WalletFixture.Ben.getAddresses(zcashNetwork).sapling }) {
                    Text(text = stringResource(id = R.string.send_ben_sapling))
                }

                Spacer(Modifier.size(8.dp))

                Button({ recipientAddressString = WalletFixture.Ben.getAddresses(zcashNetwork).transparent }) {
                    Text(text = stringResource(id = R.string.send_ben_transparent))
                }
            }
        }

        Spacer(Modifier.size(8.dp))

        TextField(value = memoString, onValueChange = {
            if (Memo.isWithinMaxLength(it)) {
                memoString = it
            }
        }, label = { Text(stringResource(id = R.string.send_memo)) })

        Spacer(Modifier.fillMaxHeight(MINIMAL_WEIGHT))

        if (validation.isNotEmpty()) {
            /*
             * Note: this is not localized in that it uses the enum constant name and joins the string
             * without regard for RTL.  This will get resolved once we do proper validation for
             * the fields.
             */
            Text(validation.joinToString(", "))
        }

        Button(
            onClick = {
                val zecSendValidation = ZecSendExt.new(
                    context,
                    recipientAddressString,
                    amountZecString,
                    memoString,
                    monetarySeparators
                )

                when (zecSendValidation) {
                    is ZecSendExt.ZecSendValidation.Valid -> onSend(zecSendValidation.zecSend)
                    is ZecSendExt.ZecSendValidation.Invalid -> validation = zecSendValidation.validationErrors
                }
            },

            // Needs actual validation
            enabled = amountZecString.isNotBlank() && recipientAddressString.isNotBlank()
        ) {
            Text(stringResource(id = R.string.send_button))
        }
    }
}
