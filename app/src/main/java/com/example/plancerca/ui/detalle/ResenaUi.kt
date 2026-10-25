package com.example.plancerca.ui.detalle

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.example.plancerca.data.Resena
import com.example.plancerca.data.ResenasRepositorio
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val MAX_FOTOS = 3

@Composable
fun Estrellas(
    calificacion: Int,
    modifier: Modifier = Modifier,
    alSeleccionar: ((Int) -> Unit)? = null
) {
    Row(modifier = modifier) {
        (1..5).forEach { numero ->
            Icon(
                imageVector = if (numero <= calificacion) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "$numero estrellas",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .size(if (alSeleccionar != null) 40.dp else 18.dp)
                    .then(
                        if (alSeleccionar != null) Modifier.clickable { alSeleccionar(numero) }
                        else Modifier
                    )
            )
        }
    }
}

@Composable
fun TarjetaResena(resena: Resena, alEliminar: () -> Unit) {
    val formato = remember { SimpleDateFormat("d MMM yyyy", Locale("es")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resena.autor,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = formato.format(Date(resena.fecha)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = alEliminar) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar reseña",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Estrellas(calificacion = resena.calificacion, modifier = Modifier.padding(top = 4.dp))
            if (resena.comentario.isNotBlank()) {
                Text(
                    text = resena.comentario,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (resena.fotos.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(resena.fotos) { ruta ->
                        AsyncImage(
                            model = File(ruta),
                            contentDescription = "Foto de la reseña",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioResena(lugarId: String, alCerrar: () -> Unit) {
    val context = LocalContext.current

    var autor by rememberSaveable { mutableStateOf("") }
    var calificacion by rememberSaveable { mutableStateOf(0) }
    var comentario by rememberSaveable { mutableStateOf("") }
    var fotos by remember { mutableStateOf(listOf<String>()) }
    var archivoPendiente by remember { mutableStateOf<File?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var publicada by remember { mutableStateOf(false) }

    // Si cierras el formulario sin publicar, borra las fotos que habías tomado
    val fotosActuales by rememberUpdatedState(fotos)
    DisposableEffect(Unit) {
        onDispose {
            if (!publicada) {
                fotosActuales.forEach { ruta -> runCatching { File(ruta).delete() } }
            }
        }
    }

    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        val archivo = archivoPendiente
        if (exito && archivo != null) {
            fotos = fotos + archivo.absolutePath
        } else {
            archivo?.delete()
        }
        archivoPendiente = null
    }

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val archivo = nuevoArchivoFoto(context)
            val copiado = try {
                context.contentResolver.openInputStream(uri)?.use { entrada ->
                    archivo.outputStream().use { salida -> entrada.copyTo(salida) }
                }
                true
            } catch (e: Exception) {
                false
            }
            if (copiado) fotos = fotos + archivo.absolutePath else archivo.delete()
        }
    }

    ModalBottomSheet(
        onDismissRequest = alCerrar,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Deja tu reseña",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "¿Cómo estuvo tu visita?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Estrellas(calificacion = calificacion, alSeleccionar = { calificacion = it })

            OutlinedTextField(
                value = autor,
                onValueChange = { autor = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tu nombre (opcional)") },
                singleLine = true
            )
            OutlinedTextField(
                value = comentario,
                onValueChange = { comentario = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tu comentario") },
                minLines = 3
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        try {
                            val archivo = nuevoArchivoFoto(context)
                            archivoPendiente = archivo
                            val uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", archivo
                            )
                            camara.launch(uri)
                        } catch (e: Exception) {
                            aviso = "No se pudo abrir la cámara"
                        }
                    },
                    enabled = fotos.size < MAX_FOTOS
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tomar foto")
                }
                OutlinedButton(
                    onClick = {
                        galeria.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = fotos.size < MAX_FOTOS
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Galería")
                }
            }
            Text(
                text = "Fotos: ${fotos.size} de $MAX_FOTOS",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            if (fotos.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(fotos) { ruta ->
                        Box {
                            AsyncImage(
                                model = File(ruta),
                                contentDescription = "Foto elegida",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            IconButton(
                                onClick = {
                                    runCatching { File(ruta).delete() }
                                    fotos = fotos - ruta
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(28.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Quitar foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            aviso?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = {
                    if (calificacion == 0) {
                        aviso = "Elige de 1 a 5 estrellas"
                    } else {
                        ResenasRepositorio.agregar(
                            Resena(
                                id = UUID.randomUUID().toString(),
                                lugarId = lugarId,
                                autor = autor.trim().ifBlank { "Visitante" },
                                calificacion = calificacion,
                                comentario = comentario.trim(),
                                fotos = fotos,
                                fecha = System.currentTimeMillis()
                            )
                        )
                        publicada = true
                        alCerrar()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text("Publicar reseña")
            }
        }
    }
}

private fun nuevoArchivoFoto(context: Context): File {
    val carpeta = File(context.filesDir, "resenas").apply { mkdirs() }
    return File(carpeta, "foto_${UUID.randomUUID()}.jpg")
}