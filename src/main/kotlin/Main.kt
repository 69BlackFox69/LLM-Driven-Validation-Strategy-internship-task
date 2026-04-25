import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.RandomAccessFile
import java.io.InputStream

class ParallelDownloader(val url: String, val numThreads: Int) {
    private val client = OkHttpClient()

    suspend fun download(destinationPath: String) = coroutineScope {
        println("Intelligence: Requesting file size...")
        val fileSize = getFileSize(url)

        if (fileSize <= 0) {
            println("Error: Failed to get file size or file is empty.")
            return@coroutineScope
        }

        println("File size: $fileSize byte. Divide by $numThreads parts.")

        // Подготавливаем файл на диске
        RandomAccessFile(destinationPath, "rw").use { it.setLength(fileSize) }

        val chunkSize = fileSize / numThreads

        val jobs = (0 until numThreads).map { i ->
            val start = i * chunkSize
            val end = if (i == numThreads - 1) fileSize - 1 else (start + chunkSize - 1)

            async(Dispatchers.IO) {
                downloadChunk(i + 1, url, start, end, destinationPath)
            }
        }

        jobs.awaitAll()
        println("---")
        println("Victory! The file has been successfully compiled.: $destinationPath")
    }

    private fun getFileSize(url: String): Long {
        val request = Request.Builder().url(url).head().build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("The server responded: ${response.code}")
            response.header("Content-Length")?.toLong() ?: 0L
        }
    }

    private fun downloadChunk(id: Int, url: String, start: Long, end: Long, path: String) {
        println("Stream $id: Downloading bytes from $start to $end")

        val request = Request.Builder()
            .url(url)
            .addHeader("Range", "bytes=$start-$end")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.byteStream() ?: throw Exception("Empty response body")

            RandomAccessFile(path, "rw").use { file ->
                file.seek(start)
                body.copyTo(file)
            }
        }
        println("stream $id: Completed!")
    }

    // Утилитная функция для копирования потока байтов
    private fun InputStream.copyTo(file: RandomAccessFile) {
        val buffer = ByteArray(8192)
        var bytes = read(buffer)
        while (bytes >= 0) {
            file.write(buffer, 0, bytes)
            bytes = read(buffer)
        }
    }
}

// ТОЧКА ВХОДА
fun main() = runBlocking {
    // Вставь сюда URL своего локального файла (когда запустишь Docker)
    // Или используй прямую ссылку на какой-нибудь файл в сети для теста
    val testUrl = "http://localhost:8080/my-local-file.txt"
    val downloader = ParallelDownloader(testUrl, numThreads = 4)

    try {
        downloader.download("downloaded_file.txt")
    } catch (e: Exception) {
        println("\nError loading!")
        println("Cause: ${e.message}")
        println("\nTIP: Check if the Docker container is running and if the file is in the folder.")
    }
}