package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.data.model.Customer
import com.example.data.model.KiranaQuickItem
import com.example.data.model.TransactionEntry
import com.example.ui.components.CustomerAvatar
import com.example.ui.components.PhotoPickerSection
import com.example.ui.theme.JamaGreen
import com.example.ui.theme.JamaGreenContainer
import com.example.ui.theme.UdharCrimson
import com.example.ui.theme.UdharCrimsonContainer
import com.example.util.PhotoUtils
import com.example.util.formatRupees
import com.example.util.formatTimestamp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomerDetailScreen(
    customer: Customer,
    transactions: List<TransactionEntry>,
    quickItems: List<KiranaQuickItem>,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onRecordTransaction: (type: String, amount: Long, items: List<String>, note: String, photoBase64: String) -> Unit,
    onUpdateProfile: (name: String, phone: String, photoBase64: String) -> Unit,
    onDeleteTransaction: (TransactionEntry) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var activeSheetType by remember { mutableStateOf<String?>(null) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var fullScreenPhotoBase64 by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CustomerAvatar(
                            name = customer.name,
                            photoBase64 = customer.photoBase64,
                            size = 40.dp,
                            modifier = Modifier.clickable {
                                if (customer.photoBase64.isNotBlank() && !customer.photoBase64.startsWith("avatar:")) {
                                    fullScreenPhotoBase64 = customer.photoBase64
                                } else {
                                    showEditProfileDialog = true
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = customer.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (customer.phone.isNotBlank()) {
                                Text(
                                    text = customer.phone,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (customer.phone.any { it.isDigit() }) {
                        IconButton(
                            onClick = {
                                val dialIntent = Intent(Intent.ACTION_DIAL, "tel:${customer.phone}".toUri())
                                context.startActivity(dialIntent)
                            }
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Call Customer")
                        }
                    }
                    IconButton(
                        onClick = {
                            val shareText = buildString {
                                append("🙏 नमस्ते ${customer.name} जी,\n")
                                append("संतोष किराना स्टोर (Santosh Kirana Store) में आपका कुल उधार बाकी: ${formatRupees(customer.netBalance)} है।\n")
                                if (customer.lastItemsSummary.isNotBlank()) {
                                    append("आखिरी सामान: ${customer.lastItemsSummary}\n")
                                }
                                append("कृपया सुविधानुसार भुगतान करें। धन्यवाद!")
                            }
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Remind via WhatsApp / SMS"))
                        },
                        modifier = Modifier.testTag("share_reminder_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Udhar Reminder")
                    }
                    IconButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier.testTag("edit_customer_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Customer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { activeSheetType = TransactionEntry.TYPE_UDHAR },
                        colors = ButtonDefaults.buttonColors(containerColor = UdharCrimson),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("give_udhar_button")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "उधार दें (Udhar)",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { activeSheetType = TransactionEntry.TYPE_JAMA },
                        colors = ButtonDefaults.buttonColors(containerColor = JamaGreen),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("receive_jama_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "जमा लें (Jama)",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CustomerBalanceSummaryCard(customer = customer)
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "लेनदेन का इतिहास (History • ${transactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "अभी कोई उधार या जमा एंट्री नहीं है",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "नीचे 'उधार दें' या 'जमा लें' बटन दबाकर हिसाब शुरू करें",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    TransactionHistoryItemCard(
                        tx = tx,
                        showCustomerName = false,
                        onPhotoClick = { b64 -> fullScreenPhotoBase64 = b64 },
                        onDeleteClick = { onDeleteTransaction(tx) }
                    )
                }
            }
        }
    }

    if (activeSheetType != null) {
        RecordTransactionBottomSheet(
            customer = customer,
            initialType = activeSheetType!!,
            quickItems = quickItems,
            isSubmitting = isSubmitting,
            onDismiss = { activeSheetType = null },
            onConfirm = { type, amount, items, note, photoBase64 ->
                onRecordTransaction(type, amount, items, note, photoBase64)
                activeSheetType = null
            }
        )
    }

    if (showEditProfileDialog) {
        EditCustomerProfileDialog(
            customer = customer,
            onDismiss = { showEditProfileDialog = false },
            onSave = { newName, newPhone, newPhoto ->
                onUpdateProfile(newName, newPhone, newPhoto)
                showEditProfileDialog = false
            }
        )
    }

    if (fullScreenPhotoBase64 != null) {
        val bmp = remember(fullScreenPhotoBase64) {
            PhotoUtils.decodeBase64ToImageBitmap(fullScreenPhotoBase64!!)
        }
        AlertDialog(
            onDismissRequest = { fullScreenPhotoBase64 = null },
            confirmButton = {
                TextButton(onClick = { fullScreenPhotoBase64 = null }) {
                    Text("बंद करें (Close)")
                }
            },
            title = { Text("फोटो प्रमाण (Saved Photo)") },
            text = {
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = "Full photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        )
    }
}

@Composable
private fun CustomerBalanceSummaryCard(customer: Customer) {
    val isUdharDue = customer.netBalance > 0L
    val isAdvance = customer.netBalance < 0L
    val statusColor = when {
        isUdharDue -> UdharCrimson
        isAdvance -> JamaGreen
        else -> MaterialTheme.colorScheme.primary
    }
    val statusBg = when {
        isUdharDue -> UdharCrimsonContainer
        isAdvance -> JamaGreenContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val statusLabel = when {
        isUdharDue -> "उधार बाकी (You Will Get)"
        isAdvance -> "एडवांस जमा (Advance)"
        else -> "हिसाब चुकता (Settled)"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Surface(
                        color = statusBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatRupees(customer.netBalance),
                        style = MaterialTheme.typography.displaySmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                CustomerAvatar(
                    name = customer.name,
                    photoBase64 = customer.photoBase64,
                    size = 64.dp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "कुल उधार (Total Udhar)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatRupees(customer.totalUdhar),
                        style = MaterialTheme.typography.titleMedium,
                        color = UdharCrimson,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "कुल जमा (Total Jama)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatRupees(customer.totalJama),
                        style = MaterialTheme.typography.titleMedium,
                        color = JamaGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionHistoryItemCard(
    tx: TransactionEntry,
    showCustomerName: Boolean,
    onPhotoClick: (String) -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    val isUdhar = tx.type == TransactionEntry.TYPE_UDHAR
    val amountColor = if (isUdhar) UdharCrimson else JamaGreen
    val badgeBg = if (isUdhar) UdharCrimsonContainer else JamaGreenContainer
    val decodedPhoto = remember(tx.photoBase64) {
        PhotoUtils.decodeBase64ToImageBitmap(tx.photoBase64)
    }
    val presetEmoji = remember(tx.photoBase64) {
        PhotoUtils.getPresetEmoji(tx.photoBase64)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Photo or Type Badge
                    if (decodedPhoto != null) {
                        Image(
                            bitmap = decodedPhoto,
                            contentDescription = "Transaction photo",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onPhotoClick(tx.photoBase64) },
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    } else if (presetEmoji != null) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(badgeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = presetEmoji, style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column {
                        if (showCustomerName) {
                            Text(
                                text = tx.customerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = badgeBg,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isUdhar) "उधार (Udhar)" else "जमा (Jama)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = amountColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formatTimestamp(tx.createdAt?.toDate()),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = (if (isUdhar) "- " else "+ ") + formatRupees(tx.amount),
                        style = MaterialTheme.typography.titleLarge,
                        color = amountColor,
                        fontWeight = FontWeight.Bold
                    )
                    if (onDeleteClick != null) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete transaction",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (tx.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tx.items.forEach { itemName ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = itemName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (tx.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "📝 ${tx.note}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EditCustomerProfileDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(customer.name) }
    var phone by remember { mutableStateOf(customer.phone) }
    var photoBase64 by remember { mutableStateOf(customer.photoBase64) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ग्राहक प्रोफाइल एडिट करें") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PhotoPickerSection(
                    currentPhotoBase64 = photoBase64,
                    onPhotoSelected = { photoBase64 = it },
                    label = "ग्राहक की फोटो"
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("नाम (Customer Name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("मोबाइल / पता (Phone / Note)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, phone, photoBase64) },
                enabled = name.isNotBlank()
            ) {
                Text("सेव करें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}
