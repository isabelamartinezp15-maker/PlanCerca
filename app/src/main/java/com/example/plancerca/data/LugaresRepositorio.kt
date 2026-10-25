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

    // Lista de Explorar: los más cercanos de cada categoría
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

    // Referencia de la foto de cada lugar, y las fotos ya resueltas
    private val fotos = mutableMapOf<String, PhotoMetadata>()
    private val fotosResueltas = mutableMapOf<String, String>()

    private const val RADIO_EXPLORAR_METROS = 2000.0
    private const val POR_CATEGORIA_EXPLORAR = 6
    private const val RADIO_MAPA_METROS = 2500.0
    private const val POR_CATEGORIA_MAPA = 20

    // Los tipos específicos van primero: si un lugar es bar y restaurante a la vez,
    // aparece como bar, que es la categoría más específica.
    private val TIPOS = listOf("park", "bar", "cafe", "tourist_attraction", "restaurant")

    private val CAMPOS = listOf(
        Place.Field.ID,
        Place.Field.DISPLAY_NAME,
        Place.Field.FORMATTED_ADDRESS,
        Place.Field.LOCATION,
        Place.Field.PRIMARY_TYPE_DISPLAY_NAME,
        Place.Field.PHOTO_METADATAS
    )

    // ---------- EXPLORAR ----------

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
            val nuevos = buscarPorTipos(
                context.applicationContext, lat, lon,
                RADIO_EXPLORAR_METROS, POR_CATEGORIA_EXPLORAR, lanzarSiFallanTodas = true
            )
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

    // ---------- MAPA ----------

    suspend fun cargarMapa(context: Context, lat: Double, lon: Double) {
        if (cargandoMapa) return
        cargandoMapa = true
        try {
            val nuevos = buscarPorTipos(
                context.applicationContext, lat, lon,
                RADIO_MAPA_METROS, POR_CATEGORIA_MAPA, lanzarSiFallanTodas = false
            )
            if (nuevos.isNotEmpty()) lugaresMapa = nuevos
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("PlanCerca", "Error cargando el mapa: ${e.message}", e)
        } finally {
            cargandoMapa = false
        }
    }

    // ---------- BÚSQUEDA: una consulta por categoría ----------

    private suspend fun buscarPorTipos(
        context: Context,
        lat: Double,
        lon: Double,
        radio: Double,
        porTipo: Int,
        lanzarSiFallanTodas: Boolean
    ): List<Lugar> {
        val cliente = obtenerCliente(context)
        val area = CircularBounds.newInstance(LatLng(lat, lon), radio)

        var fallos = 0
        var ultimoError: Exception? = null

        val encontrados = coroutineScope {
            TIPOS.map { tipo ->
                async {
                    try {
                        val peticion = SearchNearbyRequest.builder(area, CAMPOS)
                            .setIncludedTypes(listOf(tipo))
                            .setMaxResultCount(porTipo)
                            .setRankPreference(SearchNearbyRequest.RankPreference.DISTANCE)
                            .build()
                        cliente.searchNearby(peticion).esperar().places.map { tipo to it }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w("PlanCerca", "Falló la búsqueda de $tipo: ${e.message}")
                        fallos++
                        ultimoError = e
                        emptyList<Pair<String, Place>>()
                    }
                }
            }.awaitAll().flatten()
        }

        if (lanzarSiFallanTodas && fallos == TIPOS.size) {
            throw ultimoError ?: IllegalStateException("Falló la búsqueda")
        }

        // Las fotos NO se piden aquí: se piden cuando cada tarjeta aparece en pantalla
        return encontrados
            .distinctBy { it.second.id }
            .mapNotNull { (tipo, lugar) ->
                guardarFoto(lugar)
                aLugar(lugar, tipo)
            }
    }

    // ---------- FOTO BAJO DEMANDA ----------

    suspend fun fotoDe(context: Context, id: String): String {
        fotosResueltas[id]?.let { return it }
        val metadatos = fotos[id] ?: return ""
        return try {
            val url = resolverFoto(obtenerCliente(context.applicationContext), metadatos)
            if (url.isNotBlank()) fotosResueltas[id] = url
            url
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ""
        }
    }

    // ---------- AYUDAS ----------

    private fun guardarFoto(lugar: Place) {
        val id = lugar.id ?: return
        val metadatos = lugar.photoMetadatas?.firstOrNull() ?: return
        fotos[id] = metadatos
    }

    private fun aLugar(lugar: Place, tipo: String): Lugar? {
        val id = lugar.id ?: return null
        val ubicacion = lugar.location ?: return null
        return Lugar(
            id = id,
            nombre = lugar.displayName ?: "Lugar sin nombre",
            categoria = lugar.primaryTypeDisplayName ?: "Lugar",
            direccion = lugar.formattedAddress.orEmpty(),
            imagenUrl = "",
            latitud = ubicacion.latitude,
            longitud = ubicacion.longitude,
            tipo = tipo
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