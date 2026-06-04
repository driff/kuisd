package dev.kuisd.builder.io

import dev.kuisd.sdui.core.SduiEnvelope
import dev.kuisd.sdui.core.SduiNode
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuilderFileStoreTest {
    private fun tempFile(suffix: String): File =
        File.createTempFile("builder-store-test", suffix).also { it.deleteOnExit() }

    private val sample = SduiEnvelope(
        schemaVersion = 1,
        screenId = "builder",
        root = SduiNode(type = "column", id = "root", children = listOf(SduiNode(type = "text", id = "text-1"))),
        variables = mapOf("k" to JsonPrimitive("v")),
        meta = mapOf("author" to "kuisd"),
    )

    @Test
    fun `write luego read recupera el envelope`() {
        val file = tempFile(".json")

        assertTrue(BuilderFileStore.write(file, sample).isSuccess)
        val read = BuilderFileStore.read(file)

        assertEquals(sample, read.getOrThrow())
    }

    @Test
    fun `write anade extension json si falta`() {
        val noExt = tempFile("") // sin sufijo .json
        val expected = File("${noExt.path}.json").also { it.deleteOnExit() }

        val written = BuilderFileStore.write(noExt, sample).getOrThrow()

        assertEquals(expected, written) // write devuelve la ruta realmente escrita (con .json)
        assertTrue(expected.exists(), "debe escribirse con extensión .json añadida")
    }

    @Test
    fun `read de archivo inexistente devuelve failure`() {
        val missing = File(tempFile("").path + "-does-not-exist.json")

        assertTrue(BuilderFileStore.read(missing).isFailure)
    }

    @Test
    fun `read de JSON corrupto devuelve failure`() {
        val file = tempFile(".json").also { it.writeText("{ not valid json") }

        assertTrue(BuilderFileStore.read(file).isFailure)
    }

    @Test
    fun `withJsonExtension es idempotente y respeta mayusculas`() {
        assertEquals("a.json", BuilderFileStore.withJsonExtension(File("a.json")).name)
        assertEquals("a.JSON", BuilderFileStore.withJsonExtension(File("a.JSON")).name)
        assertEquals("a.json", BuilderFileStore.withJsonExtension(File("a")).name)
    }
}
