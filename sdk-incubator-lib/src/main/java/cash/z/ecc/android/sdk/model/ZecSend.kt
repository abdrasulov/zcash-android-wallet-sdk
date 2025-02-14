package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.Synchronizer

data class ZecSend(val destination: WalletAddress, val amount: Zatoshi, val memo: Memo) {
    companion object
}

fun Synchronizer.send(spendingKey: UnifiedSpendingKey, send: ZecSend) = sendToAddress(
    spendingKey,
    send.amount,
    send.destination.address,
    send.memo.value
)
