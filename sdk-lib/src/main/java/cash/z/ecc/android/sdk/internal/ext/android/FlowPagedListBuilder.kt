package cash.z.ecc.android.sdk.internal.ext.android

import android.os.Handler
import android.os.Looper
import androidx.paging.Config
import androidx.paging.DataSource
import androidx.paging.PagedList
import cash.z.ecc.android.sdk.internal.Twig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.Executor

/* Adapted from LivePagedListBuilder */
class FlowPagedListBuilder<Key, Value>(
    private val dataSourceFactory: DataSource.Factory<Key, Value>,
    private val config: PagedList.Config,
    private var initialLoadKey: Key? = null,
    private var boundaryCallback: PagedList.BoundaryCallback<*>? = null,
    private val notifyContext: CoroutineDispatcher = Dispatchers.Main,
    private val fetchContext: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Creates a FlowPagedListBuilder with required parameters.
     *
     * @param dataSourceFactory DataSource factory providing DataSource generations.
     * @param pageSize List page size.
     */
    constructor(dataSourceFactory: DataSource.Factory<Key, Value>, pageSize: Int) : this(
        dataSourceFactory,
        Config(pageSize)
    )

    /**
     * Constructs the `Flow<PagedList>`.
     *
     * No work (such as loading) is done immediately, the creation of the first PagedList is
     * deferred until the Flow is collected.
     *
     * @return The Flow of PagedLists
     */
    fun build(): Flow<List<Value>> {
        return object : ComputableFlow<List<Value>>(fetchContext) {
            private lateinit var dataSource: DataSource<Key, Value>
            private lateinit var list: PagedList<Value>
            private val callback = DataSource.InvalidatedCallback { invalidate() }

            override fun compute(): PagedList<Value> {
                var initializeKey = initialLoadKey
                if (::list.isInitialized) {
                    Twig.debug { "list is initialized" }
                    @Suppress("UNCHECKED_CAST")
                    initializeKey = list.lastKey as Key
                }

                do {
                    if (::dataSource.isInitialized) {
                        dataSource.removeInvalidatedCallback(callback)
                    }

                    dataSource = dataSourceFactory.create().apply {
                        Twig.debug { "adding an invalidated callback" }
                        addInvalidatedCallback(callback)
                    }

                    list = PagedList.Builder(dataSource, config)
                        .setNotifyExecutor(notifyContext.toExecutor())
                        .setFetchExecutor(fetchContext.toExecutor())
                        .setBoundaryCallback(boundaryCallback)
                        .setInitialKey(initializeKey)
                        .build()
                } while (list.isDetached)
                return list
            }
        }.flow
    }

    private fun CoroutineDispatcher.toExecutor(): Executor {
        return when (this) {
            is ExecutorCoroutineDispatcher -> executor
            is MainCoroutineDispatcher -> MainThreadExecutor()
            else -> error("Unable to create executor based on dispatcher: $this")
        }
    }

    class MainThreadExecutor : Executor {
        private val handler = Handler(Looper.getMainLooper())
        override fun execute(runnable: Runnable) {
            handler.post(runnable)
        }
    }
}
