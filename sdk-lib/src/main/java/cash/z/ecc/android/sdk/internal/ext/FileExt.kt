@file:Suppress("ktlint:filename")

package cash.z.ecc.android.sdk.internal.ext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.DigestInputStream
import java.security.MessageDigest

internal suspend fun File.deleteSuspend() = withContext(Dispatchers.IO) { delete() }

internal suspend fun File.existsSuspend() = withContext(Dispatchers.IO) { exists() }

internal suspend fun File.mkdirsSuspend() = withContext(Dispatchers.IO) { mkdirs() }

internal suspend fun File.canWriteSuspend() = withContext(Dispatchers.IO) { canWrite() }

internal suspend fun File.renameToSuspend(dest: File) = withContext(Dispatchers.IO) { renameTo(dest) }

suspend fun File.deleteRecursivelySuspend() = withContext(Dispatchers.IO) { deleteRecursively() }

suspend fun File.listFilesSuspend(): Array<File>? = withContext(Dispatchers.IO) { listFiles() }

suspend fun File.inputStreamSuspend(): FileInputStream = withContext(Dispatchers.IO) { inputStream() }

suspend fun File.createNewFileSuspend() = withContext(Dispatchers.IO) { createNewFile() }

/**
 * Preferred buffer size. We use the same buffer size as BufferedInputStream does.
 */
private const val BUFFER_SIZE_BYTES_SIZE = 8192

/**
 * Encrypts File to SHA1 format.
 *
 * @return String SHA1 encryption of the input file
 */
suspend fun File.getSha1Hash(): String {
    return withContext(Dispatchers.IO) {
        val messageDigest = MessageDigest.getInstance("SHA-1")
        inputStreamSuspend().use { fis ->
            DigestInputStream(fis, messageDigest).use { dis ->
                val buffer = ByteArray(BUFFER_SIZE_BYTES_SIZE)
                while (dis.read(buffer) >= 0) {
                    // reading the whole buffered stream, which results in update on the message digest
                }
                return@withContext messageDigest.digest().joinToString(
                    separator = "",
                    transform = { "%02x".format(it) }
                )
            }
        }
    }
}
