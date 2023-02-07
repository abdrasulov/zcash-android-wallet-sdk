package co.electriccoin.lightwallet.client.internal

import cash.z.wallet.sdk.internal.rpc.CompactTxStreamerGrpcKt
import cash.z.wallet.sdk.internal.rpc.Service
import co.electriccoin.lightwallet.client.CoroutineLightWalletClient
import co.electriccoin.lightwallet.client.ext.BenchmarkingExt
import co.electriccoin.lightwallet.client.fixture.BlockRangeFixture
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.CompactBlockUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.lightwallet.client.model.LightWalletEndpointInfoUnsafe
import co.electriccoin.lightwallet.client.model.RawTransactionUnsafe
import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.model.SendResponseUnsafe
import com.google.protobuf.ByteString
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Implementation of CoroutineLightWalletClient using gRPC for requests to lightwalletd.
 *
 * @property singleRequestTimeout the timeout to use for non-streaming requests. When a new stub
 * is created, it will use a deadline that is after the given duration from now.
 * @property streamingRequestTimeout the timeout to use for streaming requests. When a new stub
 * is created for streaming requests, it will use a deadline that is after the given duration from
 * now.
 */
internal class CoroutineLightWalletClientImpl private constructor(
    private val channelFactory: ChannelFactory,
    private val lightWalletEndpoint: LightWalletEndpoint,
    private val singleRequestTimeout: Duration = 10.seconds,
    private val streamingRequestTimeout: Duration = 90.seconds
) : CoroutineLightWalletClient {

    private var channel = channelFactory.newChannel(lightWalletEndpoint)

    override fun getBlockRange(heightRange: ClosedRange<BlockHeightUnsafe>): Flow<Response<CompactBlockUnsafe>> {
        require(!heightRange.isEmpty()) {
            "${Constants.ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE} range: $heightRange." // NON-NLS
        }

        return try {
            requireChannel().createStub(streamingRequestTimeout)
                .getBlockRange(heightRange.toBlockRange())
                .map {
                    Response.Success(CompactBlockUnsafe.new(it))
                }
                .catch {
                    val failure: Response.Failure<CompactBlockUnsafe> = GrpcStatusResolver.resolveFailureFromStatus(it)
                    flowOf(failure)
                }
        } catch (e: StatusRuntimeException) {
            flowOf(GrpcStatusResolver.resolveFailureFromStatus(e))
        }
    }

    override suspend fun getLatestBlockHeight(): Response<BlockHeightUnsafe> {
        return try {
            if (BenchmarkingExt.isBenchmarking()) {
                // We inject a benchmark test blocks range at this point to process only a restricted range of blocks
                // for a more reliable benchmark results.
                Response.Success(BlockHeightUnsafe(BlockRangeFixture.new().endInclusive))
            } else {
                val response = requireChannel().createStub(singleRequestTimeout)
                    .getLatestBlock(Service.ChainSpec.newBuilder().build())

                val blockHeight = BlockHeightUnsafe(response.height)

                Response.Success(blockHeight)
            }
        } catch (e: StatusRuntimeException) {
            GrpcStatusResolver.resolveFailureFromStatus(e)
        }
    }

    @Suppress("SwallowedException")
    override suspend fun getServerInfo(): Response<LightWalletEndpointInfoUnsafe> {
        return try {
            val lightdInfo = requireChannel().createStub(singleRequestTimeout)
                .getLightdInfo(Service.Empty.newBuilder().build())

            val lightwalletEndpointInfo = LightWalletEndpointInfoUnsafe.new(lightdInfo)

            Response.Success(lightwalletEndpointInfo)
        } catch (e: StatusRuntimeException) {
            GrpcStatusResolver.resolveFailureFromStatus(e)
        }
    }

    override suspend fun submitTransaction(spendTransaction: ByteArray): Response<SendResponseUnsafe> {
        require(spendTransaction.isNotEmpty()) {
            "${Constants.ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE} Failed to submit transaction because it was empty, so " +
                "this request was ignored on the client-side." // NON-NLS
        }
        return try {
            val request =
                Service.RawTransaction.newBuilder().setData(ByteString.copyFrom(spendTransaction))
                    .build()
            val response = requireChannel().createStub().sendTransaction(request)

            val sendResponse = SendResponseUnsafe.new(response)

            Response.Success(sendResponse)
        } catch (e: StatusRuntimeException) {
            GrpcStatusResolver.resolveFailureFromStatus(e)
        }
    }

    override suspend fun fetchTransaction(txId: ByteArray): Response<RawTransactionUnsafe> {
        require(txId.isNotEmpty()) {
            "${Constants.ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE} Failed to start fetching the transaction with null " +
                "transaction ID, so this request was ignored on the client-side." // NON-NLS
        }
        return try {
            val request = Service.TxFilter.newBuilder().setHash(ByteString.copyFrom(txId)).build()

            val response = requireChannel().createStub().getTransaction(request)

            val transactionResponse = RawTransactionUnsafe.new(response)

            Response.Success(transactionResponse)
        } catch (e: StatusRuntimeException) {
            GrpcStatusResolver.resolveFailureFromStatus(e)
        }
    }

    override suspend fun fetchUtxos(
        tAddress: String,
        startHeight: BlockHeightUnsafe
    ): Flow<Service.GetAddressUtxosReply> {
        require(tAddress.isNotBlank()) {
            "${Constants.ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE} address: $tAddress." // NON-NLS
        }
        return requireChannel().createStub().getAddressUtxosStream(
            Service.GetAddressUtxosArg.newBuilder().setAddresses(0, tAddress)
                .setStartHeight(startHeight.value).build()
        )
    }

    override fun getTAddressTransactions(
        tAddress: String,
        blockHeightRange: ClosedRange<BlockHeightUnsafe>
    ): Flow<Service.RawTransaction> {
        require(!blockHeightRange.isEmpty() && tAddress.isNotBlank()) {
            "${Constants.ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE} range: $blockHeightRange, address: $tAddress." // NON-NLS
        }
        return requireChannel().createStub().getTaddressTxids(
            Service.TransparentAddressBlockFilter.newBuilder().setAddress(tAddress)
                .setRange(blockHeightRange.toBlockRange()).build()
        )
    }

    override fun shutdown() {
        channel.shutdown()
    }

    override fun reconnect() {
        channel.shutdown()
        channel = channelFactory.newChannel(lightWalletEndpoint)
    }

    // These make the CoroutineLightWalletClientImpl not thread safe. In the long-term, we should
    // consider making it thread safe.
    private var stateCount = 0
    private var state: ConnectivityState? = null
    private fun requireChannel(): ManagedChannel {
        state = channel.getState(false).let { new ->
            if (state == new) stateCount++ else stateCount = 0
            new
        }
        channel.resetConnectBackoff()
        return channel
    }

    companion object {
        fun new(
            channelFactory: ChannelFactory,
            lightWalletEndpoint: LightWalletEndpoint
        ): CoroutineLightWalletClientImpl {
            return CoroutineLightWalletClientImpl(channelFactory, lightWalletEndpoint)
        }
    }
}

private fun Channel.createStub(timeoutSec: Duration = 60.seconds) =
    CompactTxStreamerGrpcKt.CompactTxStreamerCoroutineStub(this, CallOptions.DEFAULT)
        .withDeadlineAfter(timeoutSec.inWholeSeconds, TimeUnit.SECONDS)

private fun BlockHeightUnsafe.toBlockHeight(): Service.BlockID =
    Service.BlockID.newBuilder().setHeight(value).build()

private fun ClosedRange<BlockHeightUnsafe>.toBlockRange(): Service.BlockRange =
    Service.BlockRange.newBuilder()
        .setStart(start.toBlockHeight())
        .setEnd(endInclusive.toBlockHeight())
        .build()
