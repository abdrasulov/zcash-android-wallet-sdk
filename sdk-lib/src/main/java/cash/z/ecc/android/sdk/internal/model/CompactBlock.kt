package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray

internal data class CompactBlock(
    val height: BlockHeight,
    val data: FirstClassByteArray
)
