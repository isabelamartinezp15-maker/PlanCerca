package com.example.plancerca.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Fila de la tabla "favoritos" de Supabase (el usuario lo pone la base de datos sola)
@Serializable
private data class FavoritoFila(
    @SerialName("lugar_id") val lugarId: String,
    val nombre: String,
    val categoria: String,
    val direccion: String,
    @SerialName("imagen_url") val imagenUrl: String = "",
    val latitud: Double,
    val longitud: Double
)

private fun FavoritoFila.aLugar() = Lugar(
    id = lugarId,
    nombre = nombre,
    categoria = categoria,
    direccion = direccion,
    imagenUrl = imagenUrl,
    latitud = latitud,
    longitud = longitud
)

private fun Lugar.aFila() = FavoritoFila(
    lugarId = id,
    nombre = nombre,
    categoria = categoria,
    direccion = direccion,
    // Si el enlace de la foto lleva una clave (key=), no se guarda en la nube;
    // la foto se vuelve a pedir al abrir el lugar.
    imagenUrl = if (imagenUrl.contains("key=")) "" else imagenUrl,
    latitud = latitud,
    longitud = longitud
)

object FavoritosRepositorio {

    var favoritos by mutableStateOf<List<Lugar>>(emptyList())
        private set

    // Mensaje por si algo falla
    var mensajeError by mutableStateOf<String?>(null)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var iniciado = false

    fun iniciar(@Suppress("UNUSED_PARAMETER") context: Context) {
        if (iniciado) return
        iniciado = true
        // Cada vez que alguien inicia o cierra sesión, se actualiza la lista
        scope.launch {
            supabase.auth.sessionStatus.collect { estado ->
                when (estado) {
                    is SessionStatus.Authenticated -> cargar()
                    is SessionStatus.NotAuthenticated -> favoritos = emptyList()
                    else -> Unit
                }
            }
        }
    }

    fun limpiarError() {
        mensajeError = null
    }

    fun esFavorito(id: String): Boolean = favoritos.any { it.id == id }

    fun alternar(lugar: Lugar) {
        // Sin sesión no se puede guardar ningún favorito
        if (supabase.auth.currentUserOrNull() == null) return

        val eraFavorito = esFavorito(lugar.id)
        val anterior = favoritos

        // Se actualiza la pantalla de una vez y luego se guarda en Supabase
        favoritos = if (eraFavorito) {
            favoritos.filterNot { it.id == lugar.id }
        } else {
            listOf(lugar) + favoritos
        }

        scope.launch {
            try {
                if (eraFavorito) {
                    supabase.from("favoritos").delete {
                        filter { eq("lugar_id", lugar.id) }
                    }
                } else {
                    supabase.from("favoritos").insert(lugar.aFila())
                }
            } catch (e: Exception) {
                favoritos = anterior
                mensajeError = "No se pudo actualizar tus favoritos. Revisa tu conexión."
            }
        }
    }

    private suspend fun cargar() {
        try {
            val filas = supabase.from("favoritos")
                .select { order("creado_en", Order.DESCENDING) }
                .decodeList<FavoritoFila>()
            favoritos = filas.map { it.aLugar() }
        } catch (e: Exception) {
            mensajeError = "No se pudieron cargar tus favoritos."
        }
    }
}