package com.example.campusgo.features.seller.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusgo.domain.model.Category
import com.example.campusgo.ui.components.CampusGoProductImage
import com.example.campusgo.ui.components.compressImageUri
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerAddProductBottomSheet(
    categories: List<Category>,
    isSavingProduct: Boolean,
    errorMessage: String?,
    onUploadAsset: (bucket: String, path: String, bytes: ByteArray, onUploaded: (String) -> Unit) -> Unit,
    onCreateProduct: (name: String, price: Double, stock: Int, categoryId: String, description: String, imageUrl: String?, id: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var prodName by remember { mutableStateOf("") }
    var prodPrice by remember { mutableStateOf("") }
    var prodStock by remember { mutableStateOf("10") }
    var prodDesc by remember { mutableStateOf("") }
    var prodImageUrl by remember { mutableStateOf("") }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    val newProductId = remember { UUID.randomUUID().toString() }
    var selectedCatId by remember(categories) {
        mutableStateOf(categories.firstOrNull()?.id ?: "7cee355d-cf67-477c-bade-fc7867ddbe2a")
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    val productPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val bytes = compressImageUri(context, selectedUri, maxDimension = 800, quality = 80)
            if (bytes != null) {
                isUploadingPhoto = true
                val path = "products/prod_${newProductId}.jpg"
                onUploadAsset("product-images", path, bytes) { uploadedUrl ->
                    prodImageUrl = uploadedUrl
                    isUploadingPhoto = false
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Cabecera superior fija
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(38.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Regresar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Nuevo Producto",
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Añadir a tu catálogo comercial",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Contenido con scroll
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                // Vista previa de imagen con botón para seleccionar foto de galería
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { productPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CampusGoProductImage(
                            imageUrl = prodImageUrl.takeIf { it.isNotBlank() },
                            categoryName = categories.find { it.id == selectedCatId }?.name,
                            productName = prodName,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        if (isUploadingPhoto) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    ElevatedFilterChip(
                        selected = false,
                        onClick = { productPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        leadingIcon = {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = {
                            Text(
                                text = if (isUploadingPhoto) "Subiendo foto..." else if (prodImageUrl.isBlank()) "Subir foto" else "Cambiar foto",
                                fontSize = 12.sp
                            )
                        }
                    )
                }

                OutlinedTextField(
                    value = prodName,
                    onValueChange = { prodName = it; validationError = null },
                    label = { Text("Nombre del Producto *") },
                    placeholder = { Text("Ej. Triple de Pollo con Palta") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = prodPrice,
                        onValueChange = { prodPrice = it.replace(',', '.'); validationError = null },
                        label = { Text("Precio (S/.) *") },
                        placeholder = { Text("6.50") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = prodStock,
                        onValueChange = { prodStock = it.filter { ch -> ch.isDigit() }; validationError = null },
                        label = { Text("Stock inicial *") },
                        placeholder = { Text("15") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                    )
                }

                if (categories.isNotEmpty()) {
                    var expandedCat by remember { mutableStateOf(false) }
                    val currentCatName = categories.find { it.id == selectedCatId }?.name ?: "Selecciona Categoría"

                    ExposedDropdownMenuBox(
                        expanded = expandedCat,
                        onExpandedChange = { expandedCat = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentCatName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoría") },
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCat,
                            onDismissRequest = { expandedCat = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedCatId = cat.id
                                        expandedCat = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = prodDesc,
                    onValueChange = { prodDesc = it },
                    label = { Text("Descripción corta (opcional)") },
                    placeholder = { Text("Detalles para el alumno") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                val errorToDisplay = validationError ?: errorMessage
                if (errorToDisplay != null) {
                    Text(
                        text = errorToDisplay,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        val cleanPrice = prodPrice.trim().replace(',', '.')
                        val cleanStock = prodStock.trim()
                        val p = cleanPrice.toDoubleOrNull()
                        val s = cleanStock.toIntOrNull()
                        if (prodName.isBlank()) {
                            validationError = "Ingresa el nombre del producto."
                        } else if (p == null || p <= 0.0) {
                            validationError = "Ingresa un precio válido mayor a 0 (ej. 5.50)."
                        } else if (s == null || s < 0) {
                            validationError = "Ingresa una cantidad de stock válida (0 o más)."
                        } else {
                            onCreateProduct(
                                prodName,
                                p,
                                s,
                                selectedCatId.ifBlank { categories.firstOrNull()?.id ?: "7cee355d-cf67-477c-bade-fc7867ddbe2a" },
                                prodDesc.ifBlank { prodName },
                                prodImageUrl.takeIf { it.isNotBlank() },
                                newProductId
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isSavingProduct && !isUploadingPhoto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isSavingProduct) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Guardar Producto", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isSavingProduct,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
}
