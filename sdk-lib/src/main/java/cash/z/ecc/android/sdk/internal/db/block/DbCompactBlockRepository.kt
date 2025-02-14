package cash.z.ecc.android.sdk.internal.db.block

import android.content.Context
import androidx.room.RoomDatabase
import cash.z.ecc.android.sdk.internal.SdkDispatchers
import cash.z.ecc.android.sdk.internal.SdkExecutors
import cash.z.ecc.android.sdk.internal.db.commonDatabaseBuilder
import cash.z.ecc.android.sdk.internal.repository.CompactBlockRepository
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.wallet.sdk.internal.rpc.CompactFormats
import kotlinx.coroutines.withContext
import java.io.File

/**
 * An implementation of CompactBlockStore that persists information to a database in the given
 * path. This represents the "cache db" or local cache of compact blocks waiting to be scanned.
 */
class DbCompactBlockRepository private constructor(
    private val network: ZcashNetwork,
    private val cacheDb: CompactBlockDb
) : CompactBlockRepository {

    private val cacheDao = cacheDb.compactBlockDao()

    override suspend fun getLatestHeight(): BlockHeight? = runCatching {
        BlockHeight.new(network, cacheDao.latestBlockHeight())
    }.getOrNull()

    override suspend fun findCompactBlock(height: BlockHeight): CompactFormats.CompactBlock? =
        cacheDao.findCompactBlock(height.value)?.let { CompactFormats.CompactBlock.parseFrom(it) }

    override suspend fun write(result: Sequence<CompactFormats.CompactBlock>) =
        cacheDao.insert(result.map { CompactBlockEntity(it.height, it.toByteArray()) })

    override suspend fun rewindTo(height: BlockHeight) =
        cacheDao.rewindTo(height.value)

    override suspend fun close() {
        withContext(SdkDispatchers.DATABASE_IO) {
            cacheDb.close()
        }
    }

    companion object {
        /**
         * @param appContext the application context. This is used for creating the database.
         * @property databaseFile the database file.
         */
        fun new(
            appContext: Context,
            zcashNetwork: ZcashNetwork,
            databaseFile: File
        ): DbCompactBlockRepository {
            val cacheDb = createCompactBlockCacheDb(appContext.applicationContext, databaseFile)

            return DbCompactBlockRepository(zcashNetwork, cacheDb)
        }

        private fun createCompactBlockCacheDb(
            appContext: Context,
            databaseFile: File
        ): CompactBlockDb {
            return commonDatabaseBuilder(
                appContext,
                CompactBlockDb::class.java,
                databaseFile
            )
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                // this is a simple cache of blocks. destroying the db should be benign
                .fallbackToDestructiveMigration()
                .setQueryExecutor(SdkExecutors.DATABASE_IO)
                .setTransactionExecutor(SdkExecutors.DATABASE_IO)
                .build()
        }
    }
}
