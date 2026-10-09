package com.example.campusgo.features.admin.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campusgo.domain.model.CampusMeetingPoint
import com.example.campusgo.ui.components.CampusGoDialogContainerColor
import com.example.campusgo.ui.components.CampusGoDialogShape
import com.example.campusgo.ui.components.CampusGoDialogTonalElevation
import com.example.campusgo.ui.components.campusGoDialogStyle

@Composable
fun CreateMeetingPointDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, pavilion: String, description: String) -> Unit
) {
    var pointName by remember { mutableStateOf("") }
    var pavilion by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CampusGoDialogShape,
        containerColor = CampusGoDialogContainerColor,
        tonalElevation = CampusGoDialogTonalElevation,
        modifier = Modifier.campusGoDialogStyle(),
        title = {
            Text("Nuevo Punto de Encuentro Oficial", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Este punto de encuentro oficial estará disponible para la entrega de pedidos a los alumnos en el campus.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pointName,
                    onValueChange = { pointName = it },
                    label = { Text("Nombre del Punto (ej. Biblioteca)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pavilion,
                    onValueChange = { pavilion = it },
                    label = { Text("Pabellón / Sector (ej. Pabellón C)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Referencia (ej. Frente a torniquetes)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pointName, pavilion, description) },
                enabled = pointName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Guardar Punto")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DeleteMeetingPointDialog(
    point: CampusMeetingPoint,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CampusGoDialogShape,
        containerColor = CampusGoDialogContainerColor,
        tonalElevation = CampusGoDialogTonalElevation,
        modifier = Modifier.campusGoDialogStyle(),
        title = {
            Text("Eliminar Punto de Encuentro", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        },
        text = {
            Text(
                "¿Estás seguro de que deseas eliminar permanentemente el punto '${point.name}'? Ya no aparecerá en el mapa de entregas de los estudiantes.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
