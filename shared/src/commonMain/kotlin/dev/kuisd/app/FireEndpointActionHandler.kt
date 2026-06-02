package dev.kuisd.app

import dev.kuisd.app.data.ActionEndpoint
import dev.kuisd.app.data.ActionRequest
import dev.kuisd.app.data.HttpErrorMapper
import dev.kuisd.app.variables.VariableStore
import dev.kuisd.sdui.core.EndpointStatus
import dev.kuisd.sdui.core.FireEndpoint
import dev.kuisd.sdui.core.UiAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

/**
 * Ejecuta acciones [FireEndpoint] (spec 009): refleja el ciclo loading/success/error en el
 * [VariableStore] y re-despacha la respuesta del server como acciones. La red vive aquí (capa app),
 * no en el motor — el motor solo despacha la acción por `LocalSduiActionHandler`.
 *
 * El estado `loading` se escribe **síncrono** (antes de suspender) para feedback en el mismo frame;
 * la petición corre en [scope] (ligado a la entrada del back stack en el host, cancelable al salir).
 */
internal class FireEndpointActionHandler(
    private val scope: CoroutineScope,
    private val endpoint: ActionEndpoint,
    private val store: VariableStore,
) : SubHandler {
    /**
     * Re-despacho de `onSuccess`/`onError`/`response.actions`. Lo inyecta el host TRAS construir el
     * `AppActionHandler`, rompiendo el ciclo handler<->compuesto. Default no-op: el handler es
     * usable y testeable sin host.
     */
    var dispatch: (List<UiAction>) -> Unit = {}

    override fun supports(action: UiAction): Boolean = action is FireEndpoint

    override fun handle(actions: List<UiAction>) = actions.forEach { action ->
        if (action !is FireEndpoint) return@forEach
        action.statusVar?.let { store.set(it, JsonPrimitive(EndpointStatus.LOADING)) }
        val payload = action.payloadVars
            .mapNotNull { name -> store.scope.get(name)?.let { name to it } }
            .toMap()
        scope.launch {
            try {
                val response = endpoint.fire(ActionRequest(action.endpoint, action.method, payload))
                action.statusVar?.let { store.set(it, JsonPrimitive(EndpointStatus.SUCCESS)) }
                action.resultVar?.let { store.set(it, JsonPrimitive(response.message.orEmpty())) }
                dispatch(action.onSuccess + response.actions)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                action.statusVar?.let { store.set(it, JsonPrimitive(EndpointStatus.ERROR)) }
                action.resultVar?.let { store.set(it, JsonPrimitive(HttpErrorMapper.message(error))) }
                dispatch(action.onError)
            }
        }
    }
}
