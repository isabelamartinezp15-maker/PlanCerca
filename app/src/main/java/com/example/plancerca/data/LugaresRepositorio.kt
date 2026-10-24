package com.example.plancerca.data

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object LugaresRepositorio {

    var lugares by mutableStateOf<List<Lugar>>(lugaresEjemplo)
        private set
    var cargando by mutableStateOf(false)
        private set
    var mensaje by mutableStateOf<String?>(null)
        private set

    private var datosReales = false
    private var ultimaLat = 0.0
    private var ultimaLon = 0.0
    private var cliente: PlacesClient? = null

    private const val RADIO_METROS = 2000.0
    private const val MAX_RESULTADOS = 12

    suspend fun cargarCercanos(context: Context, lat: Double, lon: Double) {
        if (cargando) return
        if (datosReales) {
            val d = FloatArray(1)
            Location.distanceBetween(ultimaLat, ultimaLon, lat, lon, d)
            if (d[0] < 300f) return
        }
        cargando = true
        mensaje = null
        try {
            val nuevos = buscar(context.applicationContext, lat, lon)
            lugares = nuevos
            datosReales = true
            ultimaLat = lat
            ultimaLon = lon
            if (nuevos.isEmpty()) mensaje = "No encontramos lugares en 2 km a tu alrededor"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("PlanCerca", "Error buscando lugares: ${e.message}", e)
            mensaje = "No pudimos cargar lugares reales. Mostrando ejemplos."
        } finally {
            cargando = false
        }
    }

    private fun obtenerCliente(context: Context): PlacesClient {
        cliente?.let { return it }
        val clave = leerClave(context)
        if (clave.isBlank()) throw IllegalStateException("Falta MAPS_API_KEY")
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(context, clave)
        }
        return Places.createClient(context).also { cliente = it }
    }

    private suspend fun buscar(context: Context, lat: Double, lon: Double): List<Lugar> {
        val cliente = obtenerCliente(context)

        val campos = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION,
            Place.Field.PRIMARY_TYPE_DISPLAY_NAME,
            Place.Field.PHOTO_METADATAS
        )
        val area = CircularBounds.newInstance(LatLng(lat, lon), RADIO_METROS)
        val peticion = SearchNearbyRequest.builder(area, campos)
            .setIncludedTypes(listOf("restaurant", "cafe", "bar", "tourist_attraction"))
            .setMaxResultCount(MAX_RESULTADOS)
            .setRankPreference(SearchNearbyRequest.RankPreference.DISTANCE)
            .build()

        val encontrados = cliente.searchNearby(peticion).esperar().places

        return coroutineScope {
            encontrados.map { lugar ->
                async {
                    val id = lugar.id ?: return@async null
                    val ubicacion = lugar.location ?: return@async null
                    Lugar(
                        id = id,
                        nombre = lugar.displayName ?: "Lugar sin nombre",
                        categoria = lugar.primaryTypeDisplayName ?: "Lugar",
                        direccion = lugar.formattedAddress.orEmpty(),
                        imagenUrl = obtenerFoto(cliente, lugar),
                        latitud = ubicacion.latitude,
                        longitud = ubicacion.longitude
                    )
                }
            }.awaitAll().filterNotNull()
        }
    }

    private suspend fun obtenerFoto(cliente: PlacesClient, lugar: Place): String {
        val metadatos = lugar.photoMetadatas?.firstOrNull() ?: return ""
        return try {
            val peticion = FetchResolvedPhotoUriRequest.builder(metadatos)
                .setMaxWidth(800)
                .setMaxHeight(800)
                .build()
            cliente.fetchResolvedPhotoUri(peticion).esperar().uri?.toString().orEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("PlanCerca", "No se pudo cargar una foto: ${e.message}")
            ""
        }
    }

    // Convierte las respuestas del SDK (Task) en algo que se puede esperar con coroutines
    private suspend fun <T> Task<T>.esperar(): T = suspendCancellableCoroutine { continuacion ->
        addOnSuccessListener { continuacion.resume(it) }
        addOnFailureListener { continuacion.resumeWithException(it) }
        addOnCanceledListener { continuacion.cancel() }
    }

    // Lee la clave que ya está declarada en el manifiesto (viene de local.properties)
    private fun leerClave(context: Context): String =
        try {
            @Suppress("DEPRECATION")
            val info = context.packageManager.getApplicationInfo(
                context.packageName, PackageManager.GET_META_DATA
            )
            info.metaData?.getString("com.google.android.geo.API_KEY").orEmpty()
        } catch (e: Exception) {
            ""
        }
}