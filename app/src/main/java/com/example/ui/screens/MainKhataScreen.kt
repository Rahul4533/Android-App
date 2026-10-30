package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Customer
import com.example.data.model.KiranaQuickItem
import com.example.data.model.TransactionEntry
import com.example.data.model.UiState
import com.example.ui.components.CustomerAvatar
import com.example.ui.components.StoreSummaryHeroCard
import com.example.ui.theme.JamaGreen
import com.example.ui.theme.JamaGreenContainer
import com.example.ui.theme.UdharCrimson
import com.example.ui.theme.UdharCrimsonContainer
import com.example.ui.viewmodel.CustomerFilter
import com.example.ui.viewmodel.KhataViewModel
import com.example.util.PhotoUtils
import com.example.util.formatRupees
import com.example.util.formatTimestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainKhataScreen(
    viewModel: KhataViewModel,
    onSignOutClick: () -> Unit
) {
    val customersState by viewModel.customersState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val quickItems by viewModel.quickItems.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedCustomerId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddCustomerSheet by rememberSaveable { mutableStateOf(false) }
    var quickTxCustomer by remember { mutableStateOf<Pair<Customer, String>?>(null) }
    var editingQuickItem by remember { mutableStateOf<KiranaQuickItem?>(null) }
    var fullScreenPhotoBase64 by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val allCustomers = (customersState as? UiState.Success)?.data ?: emptyList()
    val allTransactions = (transactionsState as? UiState.Success)?.data ?: emptyList()

    val activeDetailCustomer = remember(allCustomers, selectedCustomerId) {
        allCustomers.firstOrNull { it.id == selectedCustomerId }
    }

    if (activeDetailCustomer != null) {
        val customerTxs = remember(allTransactions, activeDetailCustomer.id) {
            allTransactions.filter { it.customerId == activeDetailCustomer.id }
        }
        CustomerDetailScreen(
            customer = activeDetailCustomer,
            transactions = customerTxs,
            quickItems = quickItems,
            isSubmitting = isSubmitting,
            onBack = { selectedCustomerId = null },
            onRecordTransaction = { type, amount, items, note, photoBase64 ->
                viewModel.recordTransaction(
                    customer = activeDetailCustomer,
                    type = type,
                    amount = amount,
                    items = items,
                    note = note,
                    photoBase64 = photoBase64
                )
            },
            onUpdateProfile = { newName, newPhone, newPhoto ->
                viewModel.updateCustomerProfile(
                    customer = activeDetailCustomer,
                    newName = newName,
                    newPhone = newPhone,
                    newPhotoBase64 = newPhoto
                )
            },
            onDeleteTransaction = { tx ->
                viewModel.deleteTransaction(activeDetailCustomer, tx)
            }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(id = R.string.store_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Digital Udhar Bahi-Khata",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSignOutClick,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(id = R.string.sign_out)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showAddCustomerSheet = true },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("नया ग्राहक (+ Customer)", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_customer_fab")
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.People else Icons.Outlined.People,
                            contentDescription = "Customers"
                        )
                    },
                    label = { Text("ग्राहक खाता") },
                    modifier = Modifier.testTag("nav_customers")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "Daily History"
                        )
                    },
                    label = { Text("रोज़ाना हिसाब") },
                    modifier = Modifier.testTag("nav_history")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                            contentDescription = "Kirana Items"
                        )
                    },
                    label = { Text("किराना सामान") },
                    modifier = Modifier.testTag("nav_items")
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> CustomersKhataTab(
                customersState = customersState,
                searchQuery = searchQuery,
                selectedFilter = selectedFilter,
                onSearchChange = viewModel::updateSearchQuery,
                onFilterChange = viewModel::updateFilter,
                onCustomerClick = { selectedCustomerId = it.id },
                onQuickUdharClick = { cust -> quickTxCustomer = cust to TransactionEntry.TYPE_UDHAR },
                onQuickJamaClick = { cust -> quickTxCustomer = cust to TransactionEntry.TYPE_JAMA },
                onAddCustomerClick = { showAddCustomerSheet = true },
                modifier = Modifier.padding(innerPadding)
            )
            1 -> DailyTransactionsTab(
                transactionsState = transactionsState,
                onTransactionCustomerClick = { custId ->
                    if (allCustomers.any { it.id == custId }) {
                        selectedCustomerId = custId
                    }
                },
                onPhotoClick = { b64 -> fullScreenPhotoBase64 = b64 },
                modifier = Modifier.padding(innerPadding)
            )
            2 -> KiranaItemsRateTab(
                quickItems = quickItems,
                onEditItemClick = { editingQuickItem = it },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    if (showAddCustomerSheet) {
        AddCustomerBottomSheet(
            quickItems = quickItems,
            isSubmitting = isSubmitting,
            onDismiss = { showAddCustomerSheet = false },
            onConfirm = { name, phone, photoBase64, initialAmt, initialItems, initialNote, txPhoto ->
                viewModel.addCustomer(
                    name = name,
                    phone = phone,
                    photoBase64 = photoBase64,
                    initialUdharAmount = initialAmt,
                    initialItems = initialItems,
                    initialNote = initialNote,
                    txPhotoBase64 = txPhoto,
                    onSuccess = { showAddCustomerSheet = false }
                )
            }
        )
    }

    if (quickTxCustomer != null) {
        val (cust, initialType) = quickTxCustomer!!
        RecordTransactionBottomSheet(
            customer = cust,
            initialType = initialType,
            quickItems = quickItems,
            isSubmitting = isSubmitting,
            onDismiss = { quickTxCustomer = null },
            onConfirm = { type, amount, items, note, photoBase64 ->
                viewModel.recordTransaction(
                    customer = cust,
                    type = type,
                    amount = amount,
                    items = items,
                    note = note,
                    photoBase64 = photoBase64,
                    onSuccess = { quickTxCustomer = null }
                )
            }
        )
    }

    if (editingQuickItem != null) {
        val item = editingQuickItem!!
        var rateInput by remember(item) { mutableStateOf(item.suggestedAmount.toString()) }
        AlertDialog(
            onDismissRequest = { editingQuickItem = null },
            title = { Text("${item.emoji} ${item.hindiLabel} का रेट बदलें") },
            text = {
                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it.filter { ch -> ch.isDigit() }.take(5) },
                    label = { Text("डिफ़ॉल्ट रेट ₹ (Price in ₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = rateInput.toLongOrNull() ?: item.suggestedAmount
                        viewModel.updateQuickItemPrice(item.id, price)
                        editingQuickItem = null
                    }
                ) {
                    Text("अपडेट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingQuickItem = null }) {
                    Text("रद्द करें")
                }
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
                    Text("बंद करें")
                }
            },
            title = { Text("फोटो प्रमाण") },
            text = {
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = "Saved photo",
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
private fun CustomersKhataTab(
    customersState: UiState<List<Customer>>,
    searchQuery: String,
    selectedFilter: CustomerFilter,
    onSearchChange: (String) -> Unit,
    onFilterChange: (CustomerFilter) -> Unit,
    onCustomerClick: (Customer) -> Unit,
    onQuickUdharClick: (Customer) -> Unit,
    onQuickJamaClick: (Customer) -> Unit,
    onAddCustomerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customers = (customersState as? UiState.Success)?.data ?: emptyList()

    val totalUdharBaki = remember(customers) {
        customers.filter { it.netBalance > 0L }.sumOf { it.netBalance }
    }
    val totalJamaReceived = remember(customers) {
        customers.sumOf { it.totalJama }
    }

    val filteredCustomers = remember(customers, searchQuery, selectedFilter) {
        customers.filter { customer ->
            val matchesQuery = searchQuery.isBlank() ||
                customer.name.contains(searchQuery, ignoreCase = true) ||
                customer.phone.contains(searchQuery, ignoreCase = true) ||
                customer.lastItemsSummary.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                CustomerFilter.ALL -> true
                CustomerFilter.UDHAR_BAKI -> customer.netBalance > 0L
                CustomerFilter.SETTLED -> customer.netBalance <= 0L
            }
            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StoreSummaryHeroCard(
                totalUdharBaki = totalUdharBaki,
                totalJamaReceived = totalJamaReceived,
                activeCustomersCount = customers.size
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text(stringResource(id = R.string.search_customers_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_customers_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedFilter == CustomerFilter.ALL,
                        onClick = { onFilterChange(CustomerFilter.ALL) },
                        label = { Text("सभी (${customers.size})") }
                    )
                    FilterChip(
                        selected = selectedFilter == CustomerFilter.UDHAR_BAKI,
                        onClick = { onFilterChange(CustomerFilter.UDHAR_BAKI) },
                        label = {
                            val dueCount = customers.count { it.netBalance > 0L }
                            Text("उधार बाकी ($dueCount)")
                        }
                    )
                    FilterChip(
                        selected = selectedFilter == CustomerFilter.SETTLED,
                        onClick = { onFilterChange(CustomerFilter.SETTLED) },
                        label = {
                            val settledCount = customers.count { it.netBalance <= 0L }
                            Text("चुकता ($settledCount)")
                        }
                    )
                }
            }
        }

        when (customersState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = customersState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                if (filteredCustomers.isEmpty()) {
                    item {
                        EmptyCustomersCard(
                            hasAnyCustomers = customers.isNotEmpty(),
                            onAddCustomerClick = onAddCustomerClick
                        )
                    }
                } else {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        CustomerKhataCard(
                            customer = customer,
                            onClick = { onCustomerClick(customer) },
                            onQuickUdharClick = { onQuickUdharClick(customer) },
                            onQuickJamaClick = { onQuickJamaClick(customer) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerKhataCard(
    customer: Customer,
    onClick: () -> Unit,
    onQuickUdharClick: () -> Unit,
    onQuickJamaClick: () -> Unit
) {
    val isUdharDue = customer.netBalance > 0L
    val isAdvance = customer.netBalance < 0L
    val balanceColor = when {
        isUdharDue -> UdharCrimson
        isAdvance -> JamaGreen
        else -> MaterialTheme.colorScheme.primary
    }
    val balanceBg = when {
        isUdharDue -> UdharCrimsonContainer
        isAdvance -> JamaGreenContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val balanceTag = when {
        isUdharDue -> "बाकी (Due)"
        isAdvance -> "एडवांस"
        else -> "चुकता"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("customer_card_${customer.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomerAvatar(
                    name = customer.name,
                    photoBase64 = customer.photoBase64,
                    size = 54.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (customer.phone.isNotBlank()) {
                        Text(
                            text = "📞 ${customer.phone}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (customer.lastItemsSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "🛒 ${customer.lastItemsSummary}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatRupees(customer.netBalance),
                        style = MaterialTheme.typography.titleLarge,
                        color = balanceColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        color = balanceBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = balanceTag,
                            style = MaterialTheme.typography.labelMedium,
                            color = balanceColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick 1-Tap Counter Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(customer.updatedAt?.toDate() ?: customer.createdAt?.toDate()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = UdharCrimsonContainer,
                        modifier = Modifier
                            .clickable { onQuickUdharClick() }
                            .testTag("quick_udhar_${customer.id}")
                    ) {
                        Text(
                            text = "+ उधार (Udhar)",
                            style = MaterialTheme.typography.labelLarge,
                            color = UdharCrimson,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = JamaGreenContainer,
                        modifier = Modifier
                            .clickable { onQuickJamaClick() }
                            .testTag("quick_jama_${customer.id}")
                    ) {
                        Text(
                            text = "+ जमा (Jama)",
                            style = MaterialTheme.typography.labelLarge,
                            color = JamaGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCustomersCard(
    hasAnyCustomers: Boolean,
    onAddCustomerClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📒", fontSize = 34.sp)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (hasAnyCustomers) "कोई ग्राहक नहीं मिला" else stringResource(id = R.string.empty_customers_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasAnyCustomers) "खोज शब्द या फ़िल्टर बदलकर देखें"
                else stringResource(id = R.string.empty_customers_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!hasAnyCustomers) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onAddCustomerClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("empty_state_add_customer_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("पहला ग्राहक जोड़ें", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DailyTransactionsTab(
    transactionsState: UiState<List<TransactionEntry>>,
    onTransactionCustomerClick: (String) -> Unit,
    onPhotoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val transactions = (transactionsState as? UiState.Success)?.data ?: emptyList()

    val totalUdharCount = remember(transactions) {
        transactions.filter { it.type == TransactionEntry.TYPE_UDHAR }.sumOf { it.amount }
    }
    val totalJamaCount = remember(transactions) {
        transactions.filter { it.type == TransactionEntry.TYPE_JAMA }.sumOf { it.amount }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "कुल उधार एंट्री",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = formatRupees(totalUdharCount),
                            style = MaterialTheme.typography.titleLarge,
                            color = UdharCrimson,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "कुल जमा एंट्री",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = formatRupees(totalJamaCount),
                            style = MaterialTheme.typography.titleLarge,
                            color = JamaGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🧾", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "रोज़ाना हिसाब अभी खाली है",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "जब आप किसी ग्राहक को उधार देंगे या जमा लेंगे, तो वह फोटो और समय के साथ यहाँ दिखेगा।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(transactions, key = { it.id }) { tx ->
                Box(modifier = Modifier.clickable { onTransactionCustomerClick(tx.customerId) }) {
                    TransactionHistoryItemCard(
                        tx = tx,
                        showCustomerName = true,
                        onPhotoClick = onPhotoClick
                    )
                }
            }
        }
    }
}

@Composable
private fun KiranaItemsRateTab(
    quickItems: List<KiranaQuickItem>,
    onEditItemClick: (KiranaQuickItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "संतोष किराना स्टोर — रोज़मर्रा के सामान (Quick Items)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "उधार लिखते समय 1-टैप में सामान और उसका रेट जोड़ने के लिए यहाँ से अपनी दुकान के हिसाब से रेट सेट करें।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        items(quickItems, key = { it.id }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditItemClick(item) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = item.emoji, fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = item.hindiLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = formatRupees(item.suggestedAmount),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Rate",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
