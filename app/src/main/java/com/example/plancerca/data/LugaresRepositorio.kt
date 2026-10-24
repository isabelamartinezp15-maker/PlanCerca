package com.example.plancerca.data

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

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

    private const val RADIO_METROS = 2000.0
    private const val MAX_RESULTADOS = 12
    private const val CAMPOS =
        "places.id,places.displayName,places.formattedAddress,places.location," +
                "places.primaryTypeDisplayName,places.photos"

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
            val nuevos = withContext(Dispatchers.IO) {
                buscar(context.applicationContext, lat, lon)
            }
            lugares = nuevos
            datosReales = true
            ultimaLat = lat
            ultimaLon = lon
            if (nuevos.isEmpty()) mensaje = "No encontramos lugares en 2 km a tu alrededor"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("PlanCerca", "Error buscando lugares", e)
            mensaje = "No pudimos cargar lugares reales. Mostrando ejemplos."
        } finally {
            cargando = false
        }
    }

    private suspend fun buscar(context: Context, lat: Double, lon: Double): List<Lugar> {
        val clave = leerClave(context)
        if (clave.isBlank()) throw IOException("Falta MAPS_API_KEY")

        val cabeceras = mapOf(
            "Content-Type" to "application/json",
            "X-Goog-Api-Key" to clave,
            "X-Android-Package" to context.packageName,
            "X-Android-Cert" to huellaSha1(context)
        )

        Log.d("PlanCerca", "Enviando paquete=${context.packageName} sha1=${huellaSha1(context)}")

        val cuerpo = JSONObject()
            .put("includedTypes", JSONArray(listOf("restaurant", "cafe", "bar", "tourist_attraction")))
            .put("maxResultCount", MAX_RESULTADOS)
            .put("rankPreference", "DISTANCE")
            .put("languageCode", "es")
            .put(
                "locationRestriction",
                JSONObject().put(
                    "circle",
                    JSONObject()
                        .put("center", JSONObject().put("latitude", lat).put("longitude", lon))
                        .put("radius", RADIO_METROS)
                )
            )
            .toString()

        val respuesta = peticion(
            "https://places.googleapis.com/v1/places:searchNearby",
            "POST",
            cuerpo,
            cabeceras + ("X-Goog-FieldMask" to CAMPOS)
        )

        val lista = JSONObject(respuesta).optJSONArray("places") ?: return emptyList()

        return coroutineScope {
            (0 until lista.length()).map { i ->
                async {
                    val p = lista.getJSONObject(i)
                    val ubicacion = p.getJSONObject("location")
                    val foto = p.optJSONArray("photos")?.optJSONObject(0)?.optString("name").orEmpty()
                    Lugar(
                        id = p.getString("id"),
                        nombre = p.optJSONObject("displayName")?.optString("text").orEmpty()
                            .ifBlank { "Lugar sin nombre" },
                        categoria = p.optJSONObject("primaryTypeDisplayName")?.optString("text").orEmpty()
                            .ifBlank { "Lugar" },
                        direccion = p.optString("formattedAddress"),
                        imagenUrl = if (foto.isNotBlank()) obtenerFoto(foto, cabeceras) else "",
                        latitud = ubicacion.getDouble("latitude"),
                        longitud = ubicacion.getDouble("longitude")
                    )
                }
            }.awaitAll()
        }
    }

    // Pide a Google la dirección directa de la foto (no necesita clave para mostrarse)
    private fun obtenerFoto(nombreFoto: String, cabeceras: Map<String, String>): String =
        try {
            val url = "https://places.googleapis.com/v1/$nombreFoto/media" +
                    "?maxHeightPx=800&maxWidthPx=800&skipHttpRedirect=true"
            JSONObject(peticion(url, "GET", null, cabeceras - "Content-Type")).optString("photoUri")
        } catch (e: Exception) {
            Log.w("PlanCerca", "No se pudo cargar una foto", e)
            ""
        }

    private fun peticion(
        direccion: String,
        metodo: String,
        cuerpo: String?,
        cabeceras: Map<String, String>
    ): String {
        val conexion = URL(direccion).openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = metodo
            conexion.connectTimeout = 10_000
            conexion.readTimeout = 10_000
            cabeceras.forEach { (k, v) -> conexion.setRequestProperty(k, v) }
            if (cuerpo != null) {
                conexion.doOutput = true
                conexion.outputStream.use { it.write(cuerpo.toByteArray()) }
            }
            val codigo = conexion.responseCode
            val flujo = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
            val texto = flujo?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (codigo !in 200..299) throw IOException("HTTP $codigo: $texto")
            return texto
        } finally {
            conexion.disconnect()
        }
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

    // Tu clave está restringida a la app, así que Google pide la huella SHA-1 en cada consulta
    private fun huellaSha1(context: Context): String =
        try {
            val firmas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager
                    .getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                    .signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                context.packageManager
                    .getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                    .signatures
            }
            val firma = firmas?.firstOrNull()
            if (firma == null) "" else
                MessageDigest.getInstance("SHA-1").digest(firma.toByteArray())
                    .joinToString("") { "%02X".format(it) }
        } catch (e: Exception) {
            ""
        }
}