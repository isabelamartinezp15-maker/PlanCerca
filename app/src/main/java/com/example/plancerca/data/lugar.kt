package com.example.plancerca.data

data class Lugar(
    val id: String,
    val nombre: String,
    val categoria: String,
    val direccion: String,
    val imagenUrl: String,
    val latitud: Double,
    val longitud: Double
)