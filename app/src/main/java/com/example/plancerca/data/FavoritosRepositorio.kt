package com.example.plancerca.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

object FavoritosRepositorio {

    private const val ARCHIVO = "plancerca_prefs"
    private const val CLAVE = "favoritos"

    var favoritos by mutableStateOf<List<Lugar>>(emptyList())
        private set

    private var contexto: Context? = null

    fun iniciar(context: Context) {
        if (contexto != null) return
        contexto = context.applicationContext
        favoritos = leer()
    }

    fun esFavorito(id: String): Boolean = favoritos.any { it.id == id }

    fun alternar(lugar: Lugar) {
        favoritos = if (esFavorito(lugar.id)) {
            favoritos.filterNot { it.id == lugar.id }
        } else {
            favoritos + lugar
        }
        guardar()
    }

    private fun guardar() {
        val arreglo = JSONArray()
        favoritos.forEach { lugar ->
            arreglo.put(
                JSONObject()
                    .put("id", lugar.id)
                    .put("nombre", lugar.nombre)
                    .put("categoria", lugar.categoria)
                    .put("direccion", lugar.direccion)
                    .put("imagenUrl", lugar.imagenUrl)
                    .put("latitud", lugar.latitud)
                    .put("longitud", lugar.longitud)
            )
        }
        contexto
            ?.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(CLAVE, arreglo.toString())
            ?.apply()
    }

    private fun leer(): List<Lugar> {
        return try {
            val texto = contexto
                ?.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
                ?.getString(CLAVE, null)
                ?: return emptyList()
            val arreglo = JSONArray(texto)
            (0 until arreglo.length()).map { i ->
                val o = arreglo.getJSONObject(i)
                Lugar(
                    id = o.getString("id"),
                    nombre = o.getString("nombre"),
                    categoria = o.getString("categoria"),
                    direccion = o.getString("direccion"),
                    imagenUrl = o.optString("imagenUrl"),
                    latitud = o.getDouble("latitud"),
                    longitud = o.getDouble("longitud")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}