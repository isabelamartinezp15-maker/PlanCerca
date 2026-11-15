package com.example.plancerca.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.OffsetDateTime
import java.util.UUID

data class Resena(
    val id: String,
    val lugarId: String,
    val autor: String,
    val calificacion: Int,
    val comentario: String,
    val fotos: List<String>,
    val fecha: Long,
    val usuarioId: String = "" // NUEVO: quién la escribió
)

// Fila tal como viene de la tabla "resenas" de Supabase
@Serializable
private data class ResenaFila(
    val id: String,
    @SerialName("lugar_id") val lugarId: String,
    @SerialName("usuario_id") val usuarioId: String,
    val autor: String,
    val calificacion: Int,
    val comentario: String,
    val fotos: List<String> = emptyList(),
    @SerialName("creado_en") val creadoEn: String
)

// Datos que se envían al crear una reseña
@Serializable
private data class ResenaNueva(
    @SerialName("lugar_id") val lugarId: String,
    val autor: String,
    val calificacion: Int,
    val comentario: String,
    val fotos: List<String>
)

private fun ResenaFila.aResena() = Resena(
    id = id,
    lugarId = lugarId,
    autor = autor,
    calificacion = calificacion,
    comentario = comentario,
    fotos = fotos,
    fecha = runCatching { OffsetDateTime.parse(creadoEn).toInstant().toEpochMilli() }
        .getOrDefault(0L),
    usuarioId = usuarioId
)

object ResenasRepositorio {

    var resenas by mutableStateOf<List<Resena>>(emptyList())
        private set

    // Mensaje para mostrar si algo falla (por ahora solo se guarda)
    var mensajeError by mutableStateOf<String?>(null)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var iniciado = false

    fun iniciar(@Suppress("UNUSED_PARAMETER") context: Context) {
        if (iniciado) return
        iniciado = true
        recargar()
    }

    fun recargar() {
        scope.launch { cargar() }
    }

    fun limpiarError() {
        mensajeError = null
    }

    fun deLugar(lugarId: String): List<Resena> =
        resenas.filter { it.lugarId == lugarId }.sortedByDescending { it.fecha }

    // ¿La reseña es de la persona que tiene la sesión iniciada?
    fun esMia(resena: Resena): Boolean {
        val miId = supabase.auth.currentUserOrNull()?.id
        return miId != null && resena.usuarioId == miId
    }

    fun agregar(resena: Resena) {
        scope.launch {
            try {
                val usuarioId = supabase.auth.currentUserOrNull()?.id
                    ?: throw IllegalStateException("Sin sesión")
                val autor = AuthRepositorio.nombreActual()
                    ?.takeIf { it.isNotBlank() } ?: resena.autor

                // Primero se suben las fotos (guardadas en el celular) y se obtienen sus enlaces
                val enlaces = resena.fotos.mapNotNull { ruta -> subirFoto(usuarioId, ruta) }

                supabase.from("resenas").insert(
                    ResenaNueva(
                        lugarId = resena.lugarId,
                        autor = autor,
                        calificacion = resena.calificacion,
                        comentario = resena.comentario,
                        fotos = enlaces
                    )
                )

                // Las copias locales ya no hacen falta
                resena.fotos.forEach { ruta -> runCatching { File(ruta).delete() } }
                cargar()
            } catch (e: Exception) {
                mensajeError = "No se pudo publicar la reseña. Revisa tu conexión e inténtalo de nuevo."
            }
        }
    }

    fun eliminar(resena: Resena) {
        scope.launch {
            try {
                supabase.from("resenas").delete {
                    filter { eq("id", resena.id) }
                }
                cargar()
            } catch (e: Exception) {
                mensajeError = "No se pudo eliminar la reseña."
            }
        }
    }

    private suspend fun cargar() {
        try {
            val filas = supabase.from("resenas")
                .select {
                    order("creado_en", Order.DESCENDING)
                    limit(1000)
                }
                .decodeList<ResenaFila>()
            resenas = filas.map { it.aResena() }
        } catch (e: Exception) {
            mensajeError = "No se pudieron cargar las reseñas."
        }
    }

    // Reduce la foto, la sube al bucket "fotos" y devuelve su enlace público
    private suspend fun subirFoto(usuarioId: String, ruta: String): String? {
        val bytes = comprimir(ruta) ?: return null
        val nombre = "$usuarioId/${UUID.randomUUID()}.jpg"
        val bucket = supabase.storage.from("fotos")
        bucket.upload(nombre, bytes) {
            upsert = false
            contentType = ContentType.Image.JPEG
        }
        return bucket.publicUrl(nombre)
    }

    // Deja la foto en máximo 1600 px y calidad 80, para que pese menos de 5 MB
    private fun comprimir(ruta: String): ByteArray? {
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(ruta, medidas)
        var muestra = 1
        while (medidas.outWidth / muestra > 1600 || medidas.outHeight / muestra > 1600) {
            muestra *= 2
        }
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
        val bitmap = BitmapFactory.decodeFile(ruta, opciones) ?: return null
        val salida = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, salida)
        bitmap.recycle()
        return salida.toByteArray()
    }
}