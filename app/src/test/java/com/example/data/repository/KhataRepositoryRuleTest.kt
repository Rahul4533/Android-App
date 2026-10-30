package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.TransactionEntry
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class KhataRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createCustomerAndRecordUdhar_validPayload_updatesBalanceAndHistory() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = KhataRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCustomer(
                name = "Raju Sharma",
                phone = "9876543210",
                photoBase64 = "",
                initialUdharAmount = 60L,
                initialItems = listOf("Milk", "Bread"),
                initialNote = "Subah ka doodh bread"
            )
        }
        assertTrue(createResult.isSuccess)
        val customerId = createResult.getOrThrow()

        val fetchedCustomer = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getCustomerById(customerId).getOrThrow()
        }
        assertNotNull(fetchedCustomer)
        assertEquals(60L, fetchedCustomer!!.netBalance)

        // Record Jama (Payment)
        val jamaResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordTransaction(
                customer = fetchedCustomer,
                type = TransactionEntry.TYPE_JAMA,
                amount = 40L,
                items = emptyList(),
                note = "Cash दिया",
                photoBase64 = ""
            )
        }
        assertTrue(jamaResult.isSuccess)

        val updatedCustomer = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getCustomerById(customerId).getOrThrow()
        }
        assertNotNull(updatedCustomer)
        assertEquals(20L, updatedCustomer!!.netBalance)
        assertEquals(60L, updatedCustomer.totalUdhar)
        assertEquals(40L, updatedCustomer.totalJama)
    }

    @Test
    fun observeCustomers_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = KhataRepository(firestore)
        val customerId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCustomer(
                name = "Mohan Lal",
                phone = "9123456789",
                photoBase64 = "",
                initialUdharAmount = 100L,
                initialItems = listOf("Biscuit", "Cooking Oil")
            ).getOrThrow()
        }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCustomers().first { list -> list.any { it.id == customerId } }
        }
        assertTrue(emitted.any { it.id == customerId })
    }

    @Test
    fun getCustomerById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = KhataRepository(firestore)
        val customerId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCustomer(
                name = "Private Customer",
                phone = "9000000000",
                photoBase64 = ""
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = KhataRepository(firestore)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getCustomerById(customerId, targetUserId = aliceUid)
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user access")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
    }

    @Test
    fun observeCustomers_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = KhataRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeCustomers(userId = aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "santosh_owner@test.com"
        const val BOB_EMAIL = "other_shop@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
