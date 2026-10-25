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
import com.google.android.libraries.places.api.model.PhotoMetadata
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

    // Lista de Explorar: los más cercanos a ti
    var lugares by mutableStateOf<List<Lugar>>(lugaresEjemplo)
        private set

    // Lista del Mapa: muchos más lugares, por categoría
    var lugaresMapa by mutableStateOf<List<Lugar>>(lugaresEjemplo)
        private set

    var cargando by mutableStateOf(false)
        private set
    var cargandoMapa by mutableStateOf(false)
        private set
    var mensaje by mutableStateOf<String?>(null)
        private set

    private var datosReales = false
    private var ultimaLat = 0.0
    private var ultimaLon = 0.0
    private var cliente: PlacesClient? = null

    // Guarda la referencia de la foto de cada lugar para pedirla solo cuando se abre el detalle
    private val fotos = mutableMapOf<String, PhotoMetadata>()

    private const val RADIO_EXPLORAR_METROS = 2000.0
    private const val MAX_EXPLORAR = 12
    private const val RADIO_MAPA_METROS = 2500.0
    private const val MAX_POR_CATEGORIA = 20
    private val TIPOS = listOf("restaurant", "cafe", "bar", "tourist_attraction")

    private val CAMPOS = listOf(
        Place.Field.ID,
        Place.Field.DISPLAY_NAME,
        Place.Field.FORMATTED_ADDRESS,
        Place.Field.LOCATION,
        Place.Field.PRIMARY_TYPE_DISPLAY_NAME,
        Place.Field.PHOTO_METADATAS
    )

    // ---------- EXPLORAR: los más cercanos ----------

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
            val nuevos = buscarCercanos(context.applicationContext, lat, lon)
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

    private suspend fun buscarCercanos(context: Context, lat: Double, lon: Double): List<Lugar> {
        val cliente = obtenerCliente(context)
        val area = CircularBounds.newInstance(LatLng(lat, lon), RADIO_EXPLORAR_METROS)
        val peticion = SearchNearbyRequest.builder(area, CAMPOS)
            .setIncludedTypes(TIPOS)
            .setMaxResultCount(MAX_EXPLORAR)
            .setRankPreference(SearchNearbyRequest.RankPreference.DISTANCE)
            .build()

        val encontrados = cliente.searchNearby(peticion).esperar().places

        return coroutineScope {
            encontrados.map { lugar ->
                async {
                    val metadatos = guardarFoto(lugar)
                    val imagen = if (metadatos != null) resolverFoto(cliente, metadatos) else ""
                    aLugar(lugar, imagen)
                }
            }.awaitAll().filterNotNull()
        }
    }

    // ---------- MAPA: muchos más lugares ----------

    suspend fun cargarMapa(context: Context, lat: Double, lon: Double) {
        if (cargandoMapa) return
        cargandoMapa = true
        try {
            val nuevos = buscarMapa(context.applicationContext, lat, lon)
            if (nuevos.isNotEmpty()) lugaresMapa = nuevos
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("PlanCerca", "Error cargando el mapa: ${e.message}", e)
        } finally {
            cargandoMapa = false
        }
    }

    private suspend fun buscarMapa(context: Context, lat: Double, lon: Double): List<Lugar> {
        val cliente = obtenerCliente(context)
        val area = CircularBounds.newInstance(LatLng(lat, lon), RADIO_MAPA_METROS)

        // Una consulta por categoría, porque cada una devuelve hasta 20 resultados
        val encontrados = coroutineScope {
            TIPOS.map { tipo ->
                async {
                    try {
                        val peticion = SearchNearbyRequest.builder(area, CAMPOS)
                            .setIncludedTypes(listOf(tipo))
                            .setMaxResultCount(MAX_POR_CATEGORIA)
                            .build()
                        cliente.searchNearby(peticion).esperar().places
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w("PlanCerca", "Falló la búsqueda de $tipo: ${e.message}")
                        emptyList<Place>()
                    }
                }
            }.awaitAll().flatten()
        }

        // Sin fotos aquí: se piden solo cuando abres el detalle de un lugar
        return encontrados
            .distinctBy { it.id }
            .mapNotNull { lugar ->
                guardarFoto(lugar)
                aLugar(lugar, "")
            }
    }

    // ---------- FOTO BAJO DEMANDA (para el detalle) ----------

    suspend fun fotoDe(context: Context, id: String): String {
        val metadatos = fotos[id] ?: return ""
        return try {
            resolverFoto(obtenerCliente(context.applicationContext), metadatos)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ""
        }
    }

    // ---------- AYUDAS ----------

    private fun guardarFoto(lugar: Place): PhotoMetadata? {
        val id = lugar.id ?: return null
        val metadatos = lugar.photoMetadatas?.firstOrNull() ?: return null
        fotos[id] = metadatos
        return metadatos
    }

    private fun aLugar(lugar: Place, imagen: String): Lugar? {
        val id = lugar.id ?: return null
        val ubicacion = lugar.location ?: return null
        return Lugar(
            id = id,
            nombre = lugar.displayName ?: "Lugar sin nombre",
            categoria = lugar.primaryTypeDisplayName ?: "Lugar",
            direccion = lugar.formattedAddress.orEmpty(),
            imagenUrl = imagen,
            latitud = ubicacion.latitude,
            longitud = ubicacion.longitude
        )
    }

    private suspend fun resolverFoto(cliente: PlacesClient, metadatos: PhotoMetadata): String =
        try {
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

    private fun obtenerCliente(context: Context): PlacesClient {
        cliente?.let { return it }
        val clave = leerClave(context)
        if (clave.isBlank()) throw IllegalStateException("Falta MAPS_API_KEY")
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(context, clave)
        }
        return Places.createClient(context).also { cliente = it }
    }

    private suspend fun <T> Task<T>.esperar(): T = suspendCancellableCoroutine { continuacion ->
        addOnSuccessListener { continuacion.resume(it) }
        addOnFailureListener { continuacion.resumeWithException(it) }
        addOnCanceledListener { continuacion.cancel() }
    }

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