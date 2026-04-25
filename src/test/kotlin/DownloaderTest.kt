import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class DownloaderTest {

    @Test
    fun `test file integrity`() {
        runBlocking {
            val url = "http://localhost:8080/my-local-file.txt"
            val destination = "test_output.txt"

            val downloader = ParallelDownloader(url, numThreads = 3)
            downloader.download(destination)

            val downloadedFile = File(destination)

            assertEquals(true, downloadedFile.exists())
            assertEquals(1273L, downloadedFile.length())

            downloadedFile.delete()
        }
    }
}