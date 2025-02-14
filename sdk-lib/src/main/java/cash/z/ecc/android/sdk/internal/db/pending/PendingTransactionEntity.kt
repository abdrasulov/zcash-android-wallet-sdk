package cash.z.ecc.android.sdk.internal.db.pending

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.PendingTransaction
import cash.z.ecc.android.sdk.model.TransactionRecipient
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork

@Suppress("LongParameterList")
@Entity(tableName = "pending_transactions")
internal class PendingTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "to_address")
    val toAddress: String?,
    @ColumnInfo(name = "to_internal_account_index")
    val toInternalAccountIndex: Int?,
    val value: Long,
    val fee: Long?,
    val memo: ByteArray?,
    @ColumnInfo(name = "sent_from_account_index")
    val sentFromAccountIndex: Int,
    @ColumnInfo(name = "mined_height")
    val minedHeight: Long = NO_BLOCK_HEIGHT,
    @ColumnInfo(name = "expiry_height")
    val expiryHeight: Long = NO_BLOCK_HEIGHT,

    val cancelled: Int = 0,
    @ColumnInfo(name = "encode_attempts")
    val encodeAttempts: Int = -1,
    @ColumnInfo(name = "submit_attempts")
    val submitAttempts: Int = -1,
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,
    @ColumnInfo(name = "error_code")
    val errorCode: Int? = null,
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis(),
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val raw: ByteArray = byteArrayOf(),
    @ColumnInfo(name = "raw_transaction_id", typeAffinity = ColumnInfo.BLOB)
    val rawTransactionId: ByteArray? = byteArrayOf()
) {
    init {
        require(
            (null != toAddress && null == toInternalAccountIndex) ||
                (null == toAddress && null != toInternalAccountIndex)
        ) {
            "PendingTransaction cannot contain both a toAddress and internal account"
        }
    }

    fun toPendingTransaction(zcashNetwork: ZcashNetwork) = PendingTransaction(
        id = id,
        value = Zatoshi(value),
        fee = fee?.let { Zatoshi(it) },
        memo = memo?.let { FirstClassByteArray(it) },
        raw = FirstClassByteArray(raw),
        recipient = TransactionRecipient.new(
            toAddress,
            toInternalAccountIndex?.let { Account(toInternalAccountIndex) }
        ),
        sentFromAccount = Account(sentFromAccountIndex),
        minedHeight = if (minedHeight == NO_BLOCK_HEIGHT) {
            null
        } else {
            BlockHeight.new(zcashNetwork, minedHeight)
        },
        expiryHeight = if (expiryHeight == NO_BLOCK_HEIGHT) {
            null
        } else {
            BlockHeight.new(zcashNetwork, expiryHeight)
        },
        cancelled = cancelled,
        encodeAttempts = encodeAttempts,
        submitAttempts = submitAttempts,
        errorMessage = errorMessage,
        errorCode = errorCode,
        createTime = createTime,
        rawTransactionId = rawTransactionId?.let { FirstClassByteArray(it) }
    )

    @Suppress("ComplexMethod")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PendingTransactionEntity

        if (id != other.id) return false
        if (toAddress != other.toAddress) return false
        if (toInternalAccountIndex != other.toInternalAccountIndex) return false
        if (value != other.value) return false
        if (fee != other.fee) return false
        if (memo != null) {
            if (other.memo == null) return false
            if (!memo.contentEquals(other.memo)) return false
        } else if (other.memo != null) return false
        if (sentFromAccountIndex != other.sentFromAccountIndex) return false
        if (minedHeight != other.minedHeight) return false
        if (expiryHeight != other.expiryHeight) return false
        if (cancelled != other.cancelled) return false
        if (encodeAttempts != other.encodeAttempts) return false
        if (submitAttempts != other.submitAttempts) return false
        if (errorMessage != other.errorMessage) return false
        if (errorCode != other.errorCode) return false
        if (createTime != other.createTime) return false
        if (!raw.contentEquals(other.raw)) return false
        if (rawTransactionId != null) {
            if (other.rawTransactionId == null) return false
            if (!rawTransactionId.contentEquals(other.rawTransactionId)) return false
        } else if (other.rawTransactionId != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (toAddress?.hashCode() ?: 0)
        result = 31 * result + (toInternalAccountIndex ?: 0)
        result = 31 * result + value.hashCode()
        result = 31 * result + fee.hashCode()
        result = 31 * result + (memo?.contentHashCode() ?: 0)
        result = 31 * result + sentFromAccountIndex
        result = 31 * result + minedHeight.hashCode()
        result = 31 * result + expiryHeight.hashCode()
        result = 31 * result + cancelled
        result = 31 * result + encodeAttempts
        result = 31 * result + submitAttempts
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + (errorCode ?: 0)
        result = 31 * result + createTime.hashCode()
        result = 31 * result + raw.contentHashCode()
        result = 31 * result + (rawTransactionId?.contentHashCode() ?: 0)
        return result
    }

    companion object {
        const val NO_BLOCK_HEIGHT = -1L

        fun from(pendingTransaction: PendingTransaction): PendingTransactionEntity {
            val toAddress = if (pendingTransaction.recipient is TransactionRecipient.Address) {
                pendingTransaction.recipient.addressValue
            } else {
                null
            }
            val toInternal = if (pendingTransaction.recipient is TransactionRecipient.Account) {
                pendingTransaction.recipient.accountValue
            } else {
                null
            }

            return PendingTransactionEntity(
                id = pendingTransaction.id,
                value = pendingTransaction.value.value,
                fee = pendingTransaction.fee?.value,
                memo = pendingTransaction.memo?.byteArray,
                raw = pendingTransaction.raw.byteArray,
                toAddress = toAddress,
                toInternalAccountIndex = toInternal?.value,
                sentFromAccountIndex = pendingTransaction.sentFromAccount.value,
                minedHeight = pendingTransaction.minedHeight?.value ?: NO_BLOCK_HEIGHT,
                expiryHeight = pendingTransaction.expiryHeight?.value ?: NO_BLOCK_HEIGHT,
                cancelled = pendingTransaction.cancelled,
                encodeAttempts = pendingTransaction.encodeAttempts,
                submitAttempts = pendingTransaction.submitAttempts,
                errorMessage = pendingTransaction.errorMessage,
                errorCode = pendingTransaction.errorCode,
                createTime = pendingTransaction.createTime,
                rawTransactionId = pendingTransaction.rawTransactionId?.byteArray
            )
        }
    }
}

internal val PendingTransactionEntity.recipient: TransactionRecipient
    get() {
        return TransactionRecipient.new(toAddress, toInternalAccountIndex?.let { Account(it) })
    }

internal fun PendingTransactionEntity.isSubmitted(): Boolean {
    return submitAttempts > 0
}

internal fun PendingTransactionEntity.isFailedEncoding() = raw.isEmpty() && encodeAttempts > 0

internal fun PendingTransactionEntity.isCancelled(): Boolean {
    return cancelled > 0
}

private fun TransactionRecipient.Companion.new(
    toAddress: String?,
    toInternalAccountIndex: Account?
): TransactionRecipient {
    require(
        (null != toAddress && null == toInternalAccountIndex) ||
            (null == toAddress && null != toInternalAccountIndex)
    ) {
        "Pending transaction cannot contain both a toAddress and internal account"
    }

    if (null != toAddress) {
        return TransactionRecipient.Address(toAddress)
    } else if (null != toInternalAccountIndex) {
        return TransactionRecipient.Account(toInternalAccountIndex)
    }

    error("Pending transaction recipient require a toAddress or an internal account")
}
