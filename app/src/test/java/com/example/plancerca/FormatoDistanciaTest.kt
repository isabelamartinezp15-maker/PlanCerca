package com.example.plancerca

import com.example.plancerca.ui.ubicacion.formatearDistancia
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatoDistanciaTest {

    @Test
    fun distanciasCortasSeMuestranEnMetros() {
        assertEquals("850 m", formatearDistancia(850f))
    }

    @Test
    fun distanciasLargasSeMuestranEnKilometros() {
        assertEquals("2,3 km", formatearDistancia(2300f))
    }
}