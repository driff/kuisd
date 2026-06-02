package dev.kuisd.app

import dev.kuisd.app.data.ActionEndpoint
import dev.kuisd.app.data.ActionRequest
import dev.kuisd.app.variables.VariableStore
import dev.kuisd.sdui.core.ActionResponse
import dev.kuisd.sdui.core.EndpointStatus
import dev.kuisd.sdui.core.FireEndpoint
import dev.kuisd.sdui.core.SetVar
import dev.kuisd.sdui.core.UiAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeEndpoint(
    private val response: ActionResponse? = null,
    private val error: Throwable? = null,
) : ActionEndpoint {
    var lastRequest: ActionRequest? = null

    override suspend fun fire(request: ActionRequest): ActionResponse {
        lastRequest = request
        error?.let { throw it }
        return response ?: ActionResponse()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FireEndpointActionHandlerTest {
    private val fire = FireEndpoint(
        endpoint = "/action/greet",
        payloadVars = listOf("name"),
        statusVar = "status",
        resultVar = "result",
        onSuccess = listOf(SetVar("flag", JsonPrimitive(true))),
        onError = listOf(SetVar("errored", JsonPrimitive(true))),
    )

    @Test
    fun loading_is_set_synchronously_before_suspension() = runTest {
        val store = VariableStore().apply { seed(mapOf("name" to JsonPrimitive("Ana"))) }
        val handler = FireEndpointActionHandler(this, FakeEndpoint(ActionResponse(message = "ok")), store)

        handler.handle(listOf(fire))
        // Antes de avanzar el dispatcher, el loading ya está escrito (síncrono).
        assertEquals(JsonPrimitive(EndpointStatus.LOADING), store.vars["status"])

        advanceUntilIdle()
    }

    @Test
    fun success_sets_status_result_and_dispatches_onSuccess_plus_response_actions() = runTest {
        val store = VariableStore().apply { seed(mapOf("name" to JsonPrimitive("Ana"))) }
        val dispatched = mutableListOf<UiAction>()
        val response = ActionResponse(
            actions = listOf(SetVar("greeted", JsonPrimitive(true))),
            message = "Hola, Ana!",
        )
        val handler = FireEndpointActionHandler(this, FakeEndpoint(response), store)
        handler.dispatch = { dispatched += it }

        handler.handle(listOf(fire))
        advanceUntilIdle()

        assertEquals(JsonPrimitive(EndpointStatus.SUCCESS), store.vars["status"])
        assertEquals(JsonPrimitive("Hola, Ana!"), store.vars["result"])
        // onSuccess primero, luego las acciones del server.
        assertEquals(
            listOf<UiAction>(SetVar("flag", JsonPrimitive(true)), SetVar("greeted", JsonPrimitive(true))),
            dispatched.toList(),
        )
    }

    @Test
    fun error_sets_status_result_and_dispatches_onError() = runTest {
        val store = VariableStore().apply { seed(mapOf("name" to JsonPrimitive("Ana"))) }
        val dispatched = mutableListOf<UiAction>()
        val handler = FireEndpointActionHandler(
            this,
            FakeEndpoint(error = RuntimeException("boom")),
            store,
        )
        handler.dispatch = { dispatched += it }

        handler.handle(listOf(fire))
        advanceUntilIdle()

        assertEquals(JsonPrimitive(EndpointStatus.ERROR), store.vars["status"])
        assertTrue((store.vars["result"] as JsonPrimitive).content.isNotEmpty())
        assertEquals(listOf<UiAction>(SetVar("errored", JsonPrimitive(true))), dispatched.toList())
    }

    @Test
    fun absent_payload_var_is_omitted_from_request() = runTest {
        val store = VariableStore().apply { seed(mapOf("name" to JsonPrimitive("Ana"))) }
        val endpoint = FakeEndpoint(ActionResponse(message = "ok"))
        val handler = FireEndpointActionHandler(this, endpoint, store)
        val action = fire.copy(payloadVars = listOf("name", "missing"))

        handler.handle(listOf(action))
        advanceUntilIdle()

        val payload = endpoint.lastRequest?.payload.orEmpty()
        assertEquals(setOf("name"), payload.keys)
        assertEquals(JsonPrimitive("Ana"), payload["name"])
    }
}
