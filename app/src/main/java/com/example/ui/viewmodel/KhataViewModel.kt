package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Customer
import com.example.data.model.DEFAULT_KIRANA_ITEMS
import com.example.data.model.KiranaQuickItem
import com.example.data.model.TransactionEntry
import com.example.data.model.UiState
import com.example.data.repository.KhataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CustomerFilter {
    ALL,
    UDHAR_BAKI,
    SETTLED
}

class KhataViewModel(
    private val repository: KhataRepository,
    private val currentUserId: String
) : ViewModel() {

    val customersState: StateFlow<UiState<List<Customer>>> = repository.observeCustomers(currentUserId)
        .map<List<Customer>, UiState<List<Customer>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing customers", error)
            emit(UiState.Error(error.message ?: "Failed to load customers"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val transactionsState: StateFlow<UiState<List<TransactionEntry>>> = repository.observeAllTransactions(currentUserId)
        .map<List<TransactionEntry>, UiState<List<TransactionEntry>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing transactions", error)
            emit(UiState.Error(error.message ?: "Failed to load transactions"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(CustomerFilter.ALL)
    val selectedFilter: StateFlow<CustomerFilter> = _selectedFilter.asStateFlow()

    private val _quickItems = MutableStateFlow(DEFAULT_KIRANA_ITEMS)
    val quickItems: StateFlow<List<KiranaQuickItem>> = _quickItems.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filter: CustomerFilter) {
        _selectedFilter.value = filter
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun updateQuickItemPrice(itemId: String, newPrice: Long) {
        if (newPrice <= 0L) return
        _quickItems.value = _quickItems.value.map {
            if (it.id == itemId) it.copy(suggestedAmount = newPrice) else it
        }
        _snackbarMessage.value = "सामान का रेट अपडेट हो गया (₹$newPrice)"
    }

    fun addCustomer(
        name: String,
        phone: String,
        photoBase64: String,
        initialUdharAmount: Long,
        initialItems: List<String>,
        initialNote: String,
        txPhotoBase64: String,
        onSuccess: (String) -> Unit = {}
    ) {
        if (name.isBlank()) {
            _snackbarMessage.value = "कृपया ग्राहक का नाम लिखें"
            return
        }
        _isSubmitting.value = true
        viewModelScope.launch {
            val result = repository.createCustomer(
                name = name,
                phone = phone,
                photoBase64 = photoBase64,
                initialUdharAmount = initialUdharAmount,
                initialItems = initialItems,
                initialNote = initialNote,
                txPhotoBase64 = txPhotoBase64
            )
            _isSubmitting.value = false
            result.onSuccess { customerId ->
                _snackbarMessage.value = "${name.trim()} का खाता जुड़ गया!"
                onSuccess(customerId)
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "ग्राहक जोड़ने में त्रुटि हुई"
            }
        }
    }

    fun recordTransaction(
        customer: Customer,
        type: String,
        amount: Long,
        items: List<String>,
        note: String,
        photoBase64: String,
        onSuccess: () -> Unit = {}
    ) {
        if (amount <= 0L) {
            _snackbarMessage.value = "कृपया सही रकम (₹) दर्ज करें"
            return
        }
        _isSubmitting.value = true
        viewModelScope.launch {
            val result = repository.recordTransaction(
                customer = customer,
                type = type,
                amount = amount,
                items = items,
                note = note,
                photoBase64 = photoBase64
            )
            _isSubmitting.value = false
            result.onSuccess {
                val label = if (type == TransactionEntry.TYPE_UDHAR) "उधार ₹$amount" else "जमा ₹$amount"
                _snackbarMessage.value = "${customer.name}: $label सेव हो गया!"
                onSuccess()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "हिसाब सेव करने में त्रुटि हुई"
            }
        }
    }

    fun updateCustomerProfile(
        customer: Customer,
        newName: String,
        newPhone: String,
        newPhotoBase64: String,
        onSuccess: () -> Unit = {}
    ) {
        _isSubmitting.value = true
        viewModelScope.launch {
            val result = repository.updateCustomerProfile(
                customer = customer,
                newName = newName,
                newPhone = newPhone,
                newPhotoBase64 = newPhotoBase64
            )
            _isSubmitting.value = false
            result.onSuccess {
                _snackbarMessage.value = "ग्राहक की जानकारी अपडेट हो गई"
                onSuccess()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "अपडेट करने में त्रुटि हुई"
            }
        }
    }

    fun deleteTransaction(customer: Customer, tx: TransactionEntry) {
        viewModelScope.launch {
            val result = repository.deleteTransaction(customer, tx)
            result.onSuccess {
                _snackbarMessage.value = "एंट्री हटा दी गई"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "हटाने में त्रुटि हुई"
            }
        }
    }

    private companion object {
        const val TAG = "KhataViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
