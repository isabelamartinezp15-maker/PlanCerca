package com.example.plancerca.ui.ubicacion

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.plancerca.data.Lugar
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

class UbicacionEstado(
    val ubicacion: Location?,
    val tienePermiso: Boolean,
    val solicitarPermiso: () -> Unit
)

@Composable
fun rememberUbicacion(): UbicacionEstado {
    val context = LocalContext.current
    var tienePermiso by remember { mutableStateOf(permisoConcedido(context)) }
    var ubicacion by remember { mutableStateOf<Location?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        tienePermiso = resultado.values.any { it }
    }

    LaunchedEffect(tienePermiso) {
        if (tienePermiso) {
            obtenerUbicacion(context) { ubicacion = it }
        }
    }

    return UbicacionEstado(
        ubicacion = ubicacion,
        tienePermiso = tienePermiso,
        solicitarPermiso = {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    )
}

private fun permisoConcedido(context: Context): Boolean {
    val fina = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val aproximada = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fina || aproximada
}

@SuppressLint("MissingPermission")
private fun obtenerUbicacion(context: Context, alTerminar: (Location?) -> Unit) {
    val cliente = LocationServices.getFusedLocationProviderClient(context)
    cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
        .addOnSuccessListener { actual ->
            if (actual != null) {
                alTerminar(actual)
            } else {
                cliente.lastLocation.addOnSuccessListener { ultima -> alTerminar(ultima) }
            }
        }
        .addOnFailureListener { alTerminar(null) }
}

fun distanciaMetros(origen: Location, lugar: Lugar): Float {
    val resultado = FloatArray(1)
    Location.distanceBetween(
        origen.latitude, origen.longitude,
        lugar.latitud, lugar.longitud,
        resultado
    )
    return resultado[0]
}

fun formatearDistancia(metros: Float): String =
    if (metros < 1000) "${metros.toInt()} m"
    else String.format(Locale("es"), "%.1f km", metros / 1000)