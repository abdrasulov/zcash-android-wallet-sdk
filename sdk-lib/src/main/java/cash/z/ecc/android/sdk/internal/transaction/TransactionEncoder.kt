package cash.z.ecc.android.sdk.internal.transaction

import cash.z.ecc.android.sdk.internal.model.EncodedTransaction
import cash.z.ecc.android.sdk.model.TransactionRecipient
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.Zatoshi

internal interface TransactionEncoder {
    /**
     * Creates a transaction, throwing an exception whenever things are missing. When the provided
     * wallet implementation doesn't throw an exception, we wrap the issue into a descriptive
     * exception ourselves (rather than using double-bangs for things).
     *
     * @param usk the unified spending key associated with the notes that will be spent.
     * @param amount the amount of zatoshi to send.
     * @param toAddress the recipient's address.
     * @param memo the optional memo to include as part of the transaction.
     *
     * @return the successfully encoded transaction or an exception
     */
    suspend fun createTransaction(
        usk: UnifiedSpendingKey,
        amount: Zatoshi,
        recipient: TransactionRecipient,
        memo: ByteArray? = byteArrayOf()
    ): EncodedTransaction

    /**
     * Creates a transaction that shields any transparent funds sent to the given usk's account.
     *
     * @param usk the unified spending key associated with the transparent funds that will be shielded.
     * @param memo the optional memo to include as part of the transaction.
     */
    suspend fun createShieldingTransaction(
        usk: UnifiedSpendingKey,
        recipient: TransactionRecipient,
        memo: ByteArray? = byteArrayOf()
    ): EncodedTransaction

    /**
     * Utility function to help with validation. This is not called during [createTransaction]
     * because this class asserts that all validation is done externally by the UI, for now.
     *
     * @param address the address to validate
     *
     * @return true when the given address is a valid z-addr
     */
    suspend fun isValidShieldedAddress(address: String): Boolean

    /**
     * Utility function to help with validation. This is not called during [createTransaction]
     * because this class asserts that all validation is done externally by the UI, for now.
     *
     * @param address the address to validate
     *
     * @return true when the given address is a valid t-addr
     */
    suspend fun isValidTransparentAddress(address: String): Boolean

    /**
     * Utility function to help with validation. This is not called during [createTransaction]
     * because this class asserts that all validation is done externally by the UI, for now.
     *
     * @param address the address to validate
     *
     * @return true when the given address is a valid ZIP 316 Unified Address
     */
    suspend fun isValidUnifiedAddress(address: String): Boolean

    /**
     * Return the consensus branch that the encoder is using when making transactions.
     */
    suspend fun getConsensusBranchId(): Long
}
