package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Customer
import com.example.data.model.KiranaQuickItem
import com.example.data.model.TransactionEntry
import com.example.ui.components.PhotoPickerSection
import com.example.ui.theme.JamaGreen
import com.example.ui.theme.UdharCrimson

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCustomerBottomSheet(
    quickItems: List<KiranaQuickItem>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        phone: String,
        photoBase64: String,
        initialUdharAmount: Long,
        initialItems: List<String>,
        initialNote: String,
        txPhotoBase64: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var photoBase64 by remember { mutableStateOf("avatar:man") }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val selectedItems = remember { mutableStateListOf<String>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "नया ग्राहक खाता जोड़ें (New Customer)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "ग्राहक का नाम, फोटो और आज का उधार सामान चुनें",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            PhotoPickerSection(
                currentPhotoBase64 = photoBase64,
                onPhotoSelected = { photoBase64 = it },
                label = "ग्राहक की फोटो चुनें (Customer Photo)"
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("ग्राहक का नाम (Customer Name)*") },
                placeholder = { Text("जैसे: राजू शर्मा, पप्पू भाई, सीमा दीदी") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_name_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("मोबाइल नंबर या पहचान (Phone / Landmark)") },
                placeholder = { Text("9876543210 या गली नंबर 2") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_phone_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "पहला उधार सामान चुनें (Optional 1-Tap Items):",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickItems.forEach { item ->
                    val isSelected = selectedItems.contains(item.name)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val currentAmt = amountText.toLongOrNull() ?: 0L
                            if (isSelected) {
                                selectedItems.remove(item.name)
                                val nextAmt = (currentAmt - item.suggestedAmount).coerceAtLeast(0L)
                                amountText = if (nextAmt > 0L) nextAmt.toString() else ""
                            } else {
                                selectedItems.add(item.name)
                                val nextAmt = currentAmt + item.suggestedAmount
                                amountText = nextAmt.toString()
                            }
                        },
                        label = {
                            Text("${item.emoji} ${item.hindiLabel} (₹${item.suggestedAmount})")
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    amountText = input.filter { it.isDigit() }.take(7)
                },
                label = { Text("शुरुआती उधार रकम ₹ (Initial Udhar Amount)") },
                placeholder = { Text("0") },
                leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("initial_amount_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("विवरण / नोट (Note)") },
                placeholder = { Text("जैसे: 2 पैकेट दूध + 1 ब्रेड") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("initial_note_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("cancel_add_customer_button")
                ) {
                    Text("रद्द करें")
                }

                Button(
                    onClick = {
                        val amt = amountText.toLongOrNull() ?: 0L
                        onConfirm(
                            name,
                            phone,
                            photoBase64,
                            amt,
                            selectedItems.toList(),
                            note,
                            photoBase64
                        )
                    },
                    enabled = name.isNotBlank() && !isSubmitting,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(52.dp)
                        .testTag("save_customer_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("खाता सेव करें", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordTransactionBottomSheet(
    customer: Customer,
    initialType: String,
    quickItems: List<KiranaQuickItem>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        type: String,
        amount: Long,
        items: List<String>,
        note: String,
        photoBase64: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type by remember { mutableStateOf(initialType) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var photoBase64 by remember { mutableStateOf("") }
    val selectedItems = remember { mutableStateListOf<String>() }

    val isUdhar = type == TransactionEntry.TYPE_UDHAR
    val accentColor = if (isUdhar) UdharCrimson else JamaGreen

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp)
        ) {
            // Segmented Udhar vs Jama Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isUdhar) UdharCrimson else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { type = TransactionEntry.TYPE_UDHAR }
                        .testTag("switch_udhar_tab")
                ) {
                    Text(
                        text = "🔴 उधार दिया (Udhar)",
                        color = if (isUdhar) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (!isUdhar) JamaGreen else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { type = TransactionEntry.TYPE_JAMA }
                        .testTag("switch_jama_tab")
                ) {
                    Text(
                        text = "🟢 जमा मिला (Jama)",
                        color = if (!isUdhar) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isUdhar) "${customer.name} को उधार दें" else "${customer.name} से जमा प्राप्त करें",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isUdhar) {
                Text(
                    text = "किराना सामान पर टैप करें (Auto-adds item & rate):",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickItems.forEach { item ->
                        val isSelected = selectedItems.contains(item.name)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val currentAmt = amountText.toLongOrNull() ?: 0L
                                if (isSelected) {
                                    selectedItems.remove(item.name)
                                    val nextAmt = (currentAmt - item.suggestedAmount).coerceAtLeast(0L)
                                    amountText = if (nextAmt > 0L) nextAmt.toString() else ""
                                } else {
                                    selectedItems.add(item.name)
                                    val nextAmt = currentAmt + item.suggestedAmount
                                    amountText = nextAmt.toString()
                                }
                            },
                            label = {
                                Text("${item.emoji} ${item.hindiLabel} ₹${item.suggestedAmount}")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = UdharCrimson.copy(alpha = 0.14f),
                                selectedLabelColor = UdharCrimson
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    amountText = input.filter { it.isDigit() }.take(7)
                },
                label = { Text(if (isUdhar) "उधार रकम ₹ (Udhar Amount)*" else "जमा रकम ₹ (Jama Amount)*") },
                placeholder = { Text("जैसे: 50, 120, 500") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CurrencyRupee,
                        contentDescription = null,
                        tint = accentColor
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_amount_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Rupee Increment Chips (+₹10, +₹20, +₹50, +₹100, +₹500)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(10L, 20L, 50L, 100L, 500L).forEach { addVal ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val current = amountText.toLongOrNull() ?: 0L
                                amountText = (current + addVal).toString()
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "+₹$addVal",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("सामान या पेमेंट नोट (Details / Note)") },
                placeholder = {
                    Text(
                        if (isUdhar) "जैसे: 1L तेल, 2 बिस्कुट, ब्रेड"
                        else "जैसे: Google Pay / नकद (Cash) मिला"
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_note_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            PhotoPickerSection(
                currentPhotoBase64 = photoBase64,
                onPhotoSelected = { photoBase64 = it },
                label = "सामान / पर्ची / ग्राहक फोटो जोड़ें (Optional Photo Proof)"
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text("रद्द करें")
                }

                Button(
                    onClick = {
                        val amt = amountText.toLongOrNull() ?: 0L
                        onConfirm(type, amt, selectedItems.toList(), note, photoBase64)
                    },
                    enabled = (amountText.toLongOrNull() ?: 0L) > 0L && !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(52.dp)
                        .testTag("save_transaction_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isUdhar) "उधार सेव करें" else "जमा सेव करें",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
