package dev.kuisd.server

import dev.kuisd.sdui.core.DefaultSduiJson
import dev.kuisd.sdui.core.KUISD_VERSION_HEADER
import dev.kuisd.sdui.core.ProblemDetail
import dev.kuisd.sdui.core.SduiEnvelope
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ApplicationTest {
    @Test
    fun health_endpoint_returns_ok() = testApplication {
        application { module() }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }

    @Test
    fun screen_home_serves_a_valid_sdui_envelope() = testApplication {
        application { module() }
        val response = client.get("/screen/home")
        assertEquals(HttpStatusCode.OK, response.status)

        val envelope = DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), response.bodyAsText())
        assertEquals("home", envelope.screenId)
        assertEquals("column", envelope.root.type)
    }

    @Test
    fun screen_details_serves_a_valid_sdui_envelope() = testApplication {
        application { module() }
        val response = client.get("/screen/details")
        assertEquals(HttpStatusCode.OK, response.status)

        val envelope = DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), response.bodyAsText())
        assertEquals("details", envelope.screenId)
    }

    @Test
    fun screen_details_contains_a_badge_node() = testApplication {
        application { module() }
        val response = client.get("/screen/details")
        assertEquals(HttpStatusCode.OK, response.status)

        val envelope = DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), response.bodyAsText())
        assertTrue(
            envelope.root.children.any { it.type == "badge" },
            "esperaba un nodo type==badge en details",
        )
    }

    @Test
    fun screen_more_serves_a_valid_sdui_envelope() = testApplication {
        application { module() }
        val response = client.get("/screen/more")
        assertEquals(HttpStatusCode.OK, response.status)

        val envelope = DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), response.bodyAsText())
        assertEquals("more", envelope.screenId)
    }

    @Test
    fun unknown_screen_returns_problem_detail_404() = testApplication {
        application { module() }
        val response = client.get("/screen/does-not-exist")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(
            response.headers[HttpHeaders.ContentType].orEmpty().contains("problem+json"),
            "esperaba application/problem+json",
        )

        val problem = DefaultSduiJson.decodeFromString(ProblemDetail.serializer(), response.bodyAsText())
        assertEquals(HttpStatusCode.NotFound.value, problem.status)
    }

    @Test
    fun responses_carry_a_correlation_id() = testApplication {
        application { module() }
        val response = client.get("/health")
        assertNotNull(response.headers[HttpHeaders.XRequestId], "esperaba header de correlación")
    }

    @Test
    fun screen_counter_serves_an_envelope_with_initial_variables() = testApplication {
        application { module() }
        val response = client.get("/screen/counter")
        assertEquals(HttpStatusCode.OK, response.status)

        val envelope = DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), response.bodyAsText())
        assertEquals("counter", envelope.screenId)
        assertEquals(JsonPrimitive(0), envelope.variables["count"])
        assertEquals(JsonPrimitive(false), envelope.variables["flag"])
        assertEquals(JsonPrimitive(""), envelope.variables["name"])
        assertTrue(
            envelope.root.children.any { node ->
                node.type == "text" && node.props["text"]?.jsonPrimitive?.content == "\$count"
            },
            "esperaba un text con props.text==\"\$count\"",
        )
        assertTrue(
            envelope.root.children.any { node ->
                node.type == "textField" && node.props["bind"]?.jsonPrimitive?.content == "count"
            },
            "esperaba un textField(bind=count)",
        )
        assertTrue(
            envelope.root.children.any { node ->
                node.type == "textField" && node.props["bind"]?.jsonPrimitive?.content == "name"
            },
            "esperaba un textField(bind=name)",
        )
        val greeting = envelope.root.children.firstOrNull { it.type == "row" && it.id == "greeting" }
        assertNotNull(greeting, "esperaba un row id==greeting")
        assertTrue(
            greeting.children.any { c ->
                c.type == "text" && c.props["text"]?.jsonPrimitive?.content == "\$name"
            },
            "esperaba un text con props.text==\"\$name\" dentro del greeting",
        )
    }

    @Test
    fun screen_feed_serves_lazy_column_with_5_cards() = testApplication {
        application { module() }
        val response = client.get("/screen/feed")
        assertEquals(HttpStatusCode.OK, response.status)

        val envelope = DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), response.bodyAsText())
        assertEquals("feed", envelope.screenId)

        val lazyList = envelope.root.children.firstOrNull { it.type == "lazyColumn" }
        assertNotNull(lazyList, "esperaba un lazyColumn dentro del column raíz")
        val cards = lazyList.children.filter { it.type == "card" }
        assertEquals(5, cards.size, "esperaba 5 cards dentro del lazyColumn")
        cards.forEach { card ->
            assertEquals("color.surface", card.props["background"]?.jsonPrimitive?.content)
            assertEquals("radius.card", card.props["shape"]?.jsonPrimitive?.content)
            assertEquals("elevation.sm", card.props["elevation"]?.jsonPrimitive?.content)
        }
    }

    @Test
    fun screen_feed_header_has_iconButton_back() = testApplication {
        application { module() }
        val response = client.get("/screen/feed")
        val envelope = DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), response.bodyAsText())
        val header = envelope.root.children.firstOrNull { it.id == "header" }
        assertNotNull(header)
        val backButton = header.children.firstOrNull { it.type == "iconButton" }
        assertNotNull(backButton, "esperaba un iconButton en el header")
        assertEquals("arrowBack", backButton.props["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun screen_home_has_feed_button() = testApplication {
        application { module() }
        val response = client.get("/screen/home")
        val envelope = DefaultSduiJson.decodeFromString(SduiEnvelope.serializer(), response.bodyAsText())
        val feedButton = envelope.root.children.firstOrNull { it.id == "feed" }
        assertNotNull(feedButton, "esperaba un botón id=feed en home")
        assertEquals("button", feedButton.type)
    }

    @Test
    fun client_version_header_is_accepted() = testApplication {
        application { module() }
        val response = client.get("/screen/home") {
            header(KUISD_VERSION_HEADER, "1")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
