package cash.z.ecc.android.sdk.internal.db.block

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction

//
// Database
//

/**
 * The "Cache DB", serving as a cache of compact blocks, waiting to be processed. This will contain
 * the entire blockchain, from the birthdate of the wallet, forward. The [CompactBlockProcessor]
 * will copy blocks from this database, as they are scanned. In the future, those blocks can be
 * deleted because they are no longer needed. Currently, this efficiency has not been implemented.
 */
@Database(
    entities = [CompactBlockEntity::class],
    version = 1,
    exportSchema = true
)
abstract class CompactBlockDb : RoomDatabase() {
    abstract fun compactBlockDao(): CompactBlockDao
}

//
// Data Access Objects
//

/**
 * Data access object for compact blocks in the "Cache DB."
 */
@Dao
interface CompactBlockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(block: CompactBlockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(block: List<CompactBlockEntity>)

    @Transaction
    suspend fun insert(blocks: Sequence<CompactBlockEntity>): Int {
        var count = 0

        blocks.forEach {
            insert(it)
            count++
        }

        return count
    }

    @Query("DELETE FROM compactblocks WHERE height > :height")
    suspend fun rewindTo(height: Long)

    @Query("SELECT MAX(height) FROM compactblocks")
    suspend fun latestBlockHeight(): Long

    @Query("SELECT data FROM compactblocks WHERE height = :height")
    suspend fun findCompactBlock(height: Long): ByteArray?
}
