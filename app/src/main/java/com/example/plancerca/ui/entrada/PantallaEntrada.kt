package com.example.plancerca.ui.entrada

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Inclinación de la órbita del avión (en grados y en radianes)
private const val INCLINACION_GRADOS = -20f
private val INCLINACION = INCLINACION_GRADOS * PI.toFloat() / 180f

// Punto de la órbita (una elipse inclinada) para un ángulo t
private fun puntoOrbita(t: Float, centro: Offset, a: Float, b: Float): Offset {
    val ex = a * cos(t)
    val ey = b * sin(t)
    return Offset(
        centro.x + ex * cos(INCLINACION) - ey * sin(INCLINACION),
        centro.y + ex * sin(INCLINACION) + ey * cos(INCLINACION)
    )
}

// Pantalla de entrada: globo girando, avión en órbita y pines pequeños dentro del globo
@Composable
fun PantallaEntrada(alTerminar: () -> Unit) {
    val terminar by rememberUpdatedState(alTerminar)
    val progreso = remember { Animatable(0f) }
    val salida = remember { Animatable(1f) }

    // Línea de tiempo: 2.8 s de animación, 0.8 s de pausa y 0.4 s para desvanecerse
    LaunchedEffect(Unit) {
        progreso.animateTo(1f, tween(2800, easing = LinearEasing))
        delay(800)
        salida.animateTo(0f, tween(400))
        terminar()
    }

    val infinita = rememberInfiniteTransition(label = "entrada")
    val giroGlobo by infinita.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "globo"
    )
    val vueltaAvion by infinita.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "avion"
    )

    val colorPrincipal = MaterialTheme.colorScheme.primary

    // Medidas de la escena (en píxeles)
    val lado = with(LocalDensity.current) { 280.dp.toPx() }
    val centro = Offset(lado / 2, lado / 2)
    val radioGlobo = lado * 0.30f
    val ejeA = lado * 0.46f // ancho de la órbita
    val ejeB = lado * 0.16f // alto de la órbita

    // Hacia dónde apunta el avión (siguiendo la órbita)
    val vex = -ejeA * sin(vueltaAvion)
    val vey = ejeB * cos(vueltaAvion)
    val vx = vex * cos(INCLINACION) - vey * sin(INCLINACION)
    val vy = vex * sin(INCLINACION) + vey * cos(INCLINACION)
    val rumbo = atan2(vy, vx) * 180f / PI.toFloat()
    val avionAdelante = sin(vueltaAvion) >= 0f
    val posAvion = puntoOrbita(vueltaAvion, centro, ejeA, ejeB)

    // Posiciones de los pines dentro del globo (como fracción del tamaño de la escena)
    val pines = listOf(
        Offset(-0.14f, -0.10f),
        Offset(0.10f, -0.15f),
        Offset(0.16f, 0.02f),
        Offset(-0.19f, 0.07f),
        Offset(0.02f, 0.08f),
        Offset(-0.05f, 0.19f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = salida.value }
            .background(MaterialTheme.colorScheme.background)
            // Evita tocar la app mientras se ve la entrada
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(280.dp)) {

            // Globo, órbita y estela
            Canvas(modifier = Modifier.size(280.dp)) {
                drawCircle(
                    color = colorPrincipal.copy(alpha = 0.10f),
                    radius = radioGlobo,
                    center = centro
                )

                // Paralelos (líneas horizontales)
                listOf(-60f, -30f, 0f, 30f, 60f).forEach { latitud ->
                    val rad = latitud * PI.toFloat() / 180f
                    val y = centro.y + radioGlobo * sin(rad)
                    val mitad = radioGlobo * cos(rad)
                    drawLine(
                        color = colorPrincipal.copy(alpha = 0.55f),
                        start = Offset(centro.x - mitad, y),
                        end = Offset(centro.x + mitad, y),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                // Meridianos (elipses que giran y dan la sensación de rotación)
                for (k in 0 until 6) {
                    val angulo = giroGlobo + k * (PI.toFloat() / 6f)
                    val ancho = radioGlobo * abs(cos(angulo))
                    drawOval(
                        color = colorPrincipal.copy(alpha = 0.55f),
                        topLeft = Offset(centro.x - ancho, centro.y - radioGlobo),
                        size = Size(2 * ancho, 2 * radioGlobo),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Contorno del globo
                drawCircle(
                    color = colorPrincipal,
                    radius = radioGlobo,
                    center = centro,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Órbita del avión
                rotate(degrees = INCLINACION_GRADOS, pivot = centro) {
                    drawOval(
                        color = colorPrincipal.copy(alpha = 0.30f),
                        topLeft = Offset(centro.x - ejeA, centro.y - ejeB),
                        size = Size(2 * ejeA, 2 * ejeB),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Estela: puntos que se van haciendo pequeños y transparentes
                for (i in 1..14) {
                    val fraccion = 1f - i / 15f
                    val punto = puntoOrbita(vueltaAvion - i * 0.07f, centro, ejeA, ejeB)
                    drawCircle(
                        color = colorPrincipal.copy(alpha = 0.5f * fraccion),
                        radius = 4.dp.toPx() * fraccion,
                        center = punto
                    )
                }
            }

            // Pines pequeños que van apareciendo dentro del globo y se quedan
            pines.forEachIndexed { indice, relativo ->
                val aparece = ((progreso.value - 0.08f - indice * 0.09f) / 0.15f).coerceIn(0f, 1f)
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = colorPrincipal,
                    modifier = Modifier
                        .size(24.dp)
                        .offset {
                            IntOffset(
                                (lado / 2 + relativo.x * lado - 12.dp.toPx()).roundToInt(),
                                (lado / 2 + relativo.y * lado - 24.dp.toPx()).roundToInt()
                            )
                        }
                        .graphicsLayer {
                            scaleX = aparece
                            scaleY = aparece
                            alpha = aparece
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                )
            }

            // Avión (más pequeño y transparente cuando va por detrás del globo)
            Icon(
                imageVector = Icons.Default.Flight,
                contentDescription = null,
                tint = colorPrincipal,
                modifier = Modifier
                    .size(28.dp)
                    .offset {
                        IntOffset(
                            (posAvion.x - 14.dp.toPx()).roundToInt(),
                            (posAvion.y - 14.dp.toPx()).roundToInt()
                        )
                    }
                    .graphicsLayer {
                        // El ícono apunta hacia arriba, por eso se suma 90 grados
                        rotationZ = rumbo + 90f
                        alpha = if (avionAdelante) 1f else 0.35f
                        val escala = if (avionAdelante) 1f else 0.8f
                        scaleX = escala
                        scaleY = escala
                    }
            )
        }
    }
}