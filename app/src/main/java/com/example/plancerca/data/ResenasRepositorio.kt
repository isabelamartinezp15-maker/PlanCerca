package com.example.plancerca.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Resena(
    val id: String,
    val lugarId: String,
    val autor: String,
    val calificacion: Int,
    val comentario: String,
    val fotos: List<String>,
    val fecha: Long
)

object ResenasRepositorio {

    private const val ARCHIVO = "plancerca_prefs"
    private const val CLAVE = "resenas"

    var resenas by mutableStateOf<List<Resena>>(emptyList())
        private set

    private var contexto: Context? = null

    fun iniciar(context: Context) {
        if (contexto != null) return
        contexto = context.applicationContext
        resenas = leer()
    }

    fun deLugar(lugarId: String): List<Resena> =
        resenas.filter { it.lugarId == lugarId }.sortedByDescending { it.fecha }

    fun agregar(resena: Resena) {
        resenas = resenas + resena
        guardar()
    }

    fun eliminar(resena: Resena) {
        resena.fotos.forEach { ruta -> runCatching { File(ruta).delete() } }
        resenas = resenas.filterNot { it.id == resena.id }
        guardar()
    }

    private fun guardar() {
        val arreglo = JSONArray()
        resenas.forEach { r ->
            arreglo.put(
                JSONObject()
                    .put("id", r.id)
                    .put("lugarId", r.lugarId)
                    .put("autor", r.autor)
                    .put("calificacion", r.calificacion)
                    .put("comentario", r.comentario)
                    .put("fotos", JSONArray(r.fotos))
                    .put("fecha", r.fecha)
            )
        }
        contexto
            ?.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(CLAVE, arreglo.toString())
            ?.apply()
    }

    private fun leer(): List<Resena> {
        return try {
            val texto = contexto
                ?.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
                ?.getString(CLAVE, null)
                ?: return emptyList()
            val arreglo = JSONArray(texto)
            (0 until arreglo.length()).map { i ->
                val o = arreglo.getJSONObject(i)
                val fotosJson = o.optJSONArray("fotos") ?: JSONArray()
                Resena(
                    id = o.getString("id"),
                    lugarId = o.getString("lugarId"),
                    autor = o.optString("autor", "Visitante"),
                    calificacion = o.getInt("calificacion"),
                    comentario = o.optString("comentario"),
                    fotos = (0 until fotosJson.length()).map { fotosJson.getString(it) },
                    fecha = o.getLong("fecha")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}