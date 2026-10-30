package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val photoBase64: String = "",
    val netBalance: Long = 0L,
    val totalUdhar: Long = 0L,
    val totalJama: Long = 0L,
    val lastItemsSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TransactionEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val type: String = TYPE_UDHAR,
    val amount: Long = 0L,
    val items: List<String> = emptyList(),
    val note: String = "",
    val photoBase64: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        const val TYPE_UDHAR = "UDHAR"
        const val TYPE_JAMA = "JAMA"
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class KiranaQuickItem(
    val id: String,
    val name: String,
    val hindiLabel: String,
    val suggestedAmount: Long,
    val emoji: String
)

val DEFAULT_KIRANA_ITEMS = listOf(
    KiranaQuickItem("milk", "Milk", "दूध (Milk)", 30L, "🥛"),
    KiranaQuickItem("bread", "Bread", "ब्रेड (Bread)", 40L, "🍞"),
    KiranaQuickItem("biscuit", "Biscuit", "बिस्कुट (Biscuit)", 10L, "🍪"),
    KiranaQuickItem("oil", "Cooking Oil", "तेल (Oil)", 140L, "🫙"),
    KiranaQuickItem("gutka", "Pan / Supari", "पान / सुपारी", 10L, "🌿"),
    KiranaQuickItem("sugar", "Sugar (Chini)", "चीनी (1 Kg)", 45L, "🍚"),
    KiranaQuickItem("chai", "Tea (Chai Patti)", "चाय पत्ती", 50L, "☕"),
    KiranaQuickItem("atta", "Atta / Flour", "आटा (Atta)", 220L, "🌾"),
    KiranaQuickItem("maggi", "Noodles / Snack", "मैगी / नाश्ता", 15L, "🍜"),
    KiranaQuickItem("soap", "Soap / Surf", "साबुन / सर्फ", 35L, "🧼"),
    KiranaQuickItem("cold_drink", "Cold Drink", "कोल्ड ड्रिंक", 40L, "🥤"),
    KiranaQuickItem("dal", "Dal / Pulses", "दाल (Dal)", 120L, "🥣")
)
