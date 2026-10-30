package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Customer
import com.example.data.model.TransactionEntry
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class KhataRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeCustomers(userId: String = requireUserId()): Flow<List<Customer>> = flow {
        val path = "users/$userId/customers"
        emitAll(
            db.collection("users").document(userId).collection("customers")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Customer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAllTransactions(userId: String = requireUserId()): Flow<List<TransactionEntry>> = flow {
        val path = "users/$userId/transactions"
        emitAll(
            db.collection("users").document(userId).collection("transactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(TransactionEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeCustomerTransactions(
        customerId: String,
        userId: String = requireUserId()
    ): Flow<List<TransactionEntry>> = observeAllTransactions(userId).map { list ->
        list.filter { it.customerId == customerId }
    }

    suspend fun getCustomers(userId: String = requireUserId()): Result<List<Customer>> {
        val path = "users/$userId/customers"
        return try {
            val snapshot = db.collection("users").document(userId).collection("customers")
                .get()
                .await()
            val list = snapshot.toObjects(Customer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getCustomerById(customerId: String, targetUserId: String = requireUserId()): Result<Customer?> {
        val docRef = db.collection("users").document(targetUserId).collection("customers").document(customerId)
        return try {
            val snapshot = docRef.get().await()
            val customer = snapshot.toObject(Customer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(customer)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createCustomer(
        name: String,
        phone: String,
        photoBase64: String,
        initialUdharAmount: Long = 0L,
        initialItems: List<String> = emptyList(),
        initialNote: String = "",
        txPhotoBase64: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val trimmedName = name.trim().take(80)
        require(trimmedName.isNotEmpty()) { "Customer name is required" }

        val customerId = "cust_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val customerRef = db.collection("users").document(uid).collection("customers").document(customerId)

        val safeItems = initialItems.map { it.trim().take(50) }.filter { it.isNotEmpty() }.take(15)
        val summary = if (safeItems.isNotEmpty()) {
            safeItems.joinToString(", ").take(120)
        } else {
            initialNote.trim().take(120)
        }

        val validInitialAmount = initialUdharAmount.coerceIn(0L, 1000000L)
        val customer = Customer(
            id = customerId,
            userId = uid,
            name = trimmedName,
            phone = phone.trim().take(20),
            photoBase64 = photoBase64.take(60000),
            netBalance = validInitialAmount,
            totalUdhar = validInitialAmount,
            totalJama = 0L,
            lastItemsSummary = summary,
            createdAt = null,
            updatedAt = null
        )

        return try {
            val batch = db.batch()
            batch.set(customerRef, customer)

            if (validInitialAmount >= 1L) {
                val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(20)}"
                val txRef = db.collection("users").document(uid).collection("transactions").document(txId)
                val txEntry = TransactionEntry(
                    id = txId,
                    userId = uid,
                    customerId = customerId,
                    customerName = trimmedName,
                    type = TransactionEntry.TYPE_UDHAR,
                    amount = validInitialAmount,
                    items = safeItems,
                    note = initialNote.trim().take(200),
                    photoBase64 = txPhotoBase64.take(60000),
                    createdAt = null,
                    updatedAt = null
                )
                batch.set(txRef, txEntry)
            }

            batch.commit().await()
            Result.success(customerId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, customerRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordTransaction(
        customer: Customer,
        type: String,
        amount: Long,
        items: List<String>,
        note: String,
        photoBase64: String
    ): Result<String> {
        val uid = requireUserId()
        require(customer.id.isNotBlank()) { "Customer ID must not be blank" }
        require(type == TransactionEntry.TYPE_UDHAR || type == TransactionEntry.TYPE_JAMA) {
            "Invalid transaction type: $type"
        }
        val validAmount = amount.coerceIn(1L, 1000000L)
        val safeItems = items.map { it.trim().take(50) }.filter { it.isNotEmpty() }.take(15)

        val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val txRef = db.collection("users").document(uid).collection("transactions").document(txId)
        val customerRef = db.collection("users").document(uid).collection("customers").document(customer.id)

        val isUdhar = type == TransactionEntry.TYPE_UDHAR
        val newTotalUdhar = if (isUdhar) customer.totalUdhar + validAmount else customer.totalUdhar
        val newTotalJama = if (!isUdhar) customer.totalJama + validAmount else customer.totalJama
        val newNetBalance = (newTotalUdhar - newTotalJama).coerceIn(-10000000L, 10000000L)

        val newSummary = when {
            safeItems.isNotEmpty() -> safeItems.joinToString(", ").take(120)
            note.isNotBlank() -> note.trim().take(120)
            !isUdhar -> "Jama ₹$validAmount"
            else -> customer.lastItemsSummary
        }

        val txEntry = TransactionEntry(
            id = txId,
            userId = uid,
            customerId = customer.id,
            customerName = customer.name.take(80),
            type = type,
            amount = validAmount,
            items = safeItems,
            note = note.trim().take(200),
            photoBase64 = photoBase64.take(60000),
            createdAt = null,
            updatedAt = null
        )

        return try {
            // Fetch current customer snapshot if createdAt is null on local model to preserve immutable createdAt
            val existingCreatedAt = customer.createdAt ?: customerRef.get().await().getTimestamp("createdAt")
            requireNotNull(existingCreatedAt) { "Customer createdAt timestamp missing" }

            val updatedCustomer = customer.copy(
                netBalance = newNetBalance,
                totalUdhar = newTotalUdhar.coerceIn(0L, 100000000L),
                totalJama = newTotalJama.coerceIn(0L, 100000000L),
                lastItemsSummary = newSummary,
                createdAt = existingCreatedAt,
                updatedAt = null
            )

            val batch = db.batch()
            batch.set(txRef, txEntry)
            batch.set(customerRef, updatedCustomer)
            batch.commit().await()
            Result.success(txId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, txRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCustomerProfile(
        customer: Customer,
        newName: String,
        newPhone: String,
        newPhotoBase64: String
    ): Result<Unit> {
        val uid = requireUserId()
        require(customer.id.isNotBlank()) { "Customer ID must not be blank" }
        val customerRef = db.collection("users").document(uid).collection("customers").document(customer.id)

        return try {
            val existingCreatedAt = customer.createdAt ?: customerRef.get().await().getTimestamp("createdAt")
            requireNotNull(existingCreatedAt) { "Customer createdAt timestamp missing" }

            val updated = customer.copy(
                name = newName.trim().take(80).ifEmpty { customer.name },
                phone = newPhone.trim().take(20),
                photoBase64 = newPhotoBase64.take(60000),
                createdAt = existingCreatedAt,
                updatedAt = null
            )
            customerRef.set(updated).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, customerRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(customer: Customer, tx: TransactionEntry): Result<Unit> {
        val uid = requireUserId()
        val txRef = db.collection("users").document(uid).collection("transactions").document(tx.id)
        val customerRef = db.collection("users").document(uid).collection("customers").document(customer.id)

        return try {
            val existingCreatedAt = customer.createdAt ?: customerRef.get().await().getTimestamp("createdAt")
            requireNotNull(existingCreatedAt) { "Customer createdAt timestamp missing" }

            val isUdhar = tx.type == TransactionEntry.TYPE_UDHAR
            val newTotalUdhar = if (isUdhar) (customer.totalUdhar - tx.amount).coerceAtLeast(0L) else customer.totalUdhar
            val newTotalJama = if (!isUdhar) (customer.totalJama - tx.amount).coerceAtLeast(0L) else customer.totalJama
            val newNetBalance = (newTotalUdhar - newTotalJama).coerceIn(-10000000L, 10000000L)

            val updatedCustomer = customer.copy(
                netBalance = newNetBalance,
                totalUdhar = newTotalUdhar,
                totalJama = newTotalJama,
                createdAt = existingCreatedAt,
                updatedAt = null
            )

            val batch = db.batch()
            batch.delete(txRef)
            batch.set(customerRef, updatedCustomer)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, txRef.path)
            Result.failure(e)
        }
    }
}
