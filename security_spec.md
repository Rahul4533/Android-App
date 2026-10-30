# Security Specification — Santosh Khata (Digital Udhar Khata)

## 1. Data Invariants
1. **Multi-User Shopkeeper Isolation**: Every document in `/users/{userId}/customers/{customerId}` and `/users/{userId}/transactions/{transactionId}` belongs exclusively to `userId` (`request.auth.uid == userId` and `data.userId == request.auth.uid`).
2. **Strict Key Allowlisting**: Neither `Customer` nor `TransactionEntry` documents can contain undeclared shadow/ghost fields.
3. **Immutable Identity & Creation Timestamps**: `userId` and `createdAt` (and `customerId` on transactions) cannot be mutated during `update`.
4. **Bounded Strings & Lists**: All strings (`name`, `phone`, `photoBase64`, `lastItemsSummary`, `note`) and lists (`items` <= 15 items) have strict size caps to prevent resource exhaustion.
5. **Temporal Integrity**: `createdAt` and `updatedAt` must be valid server timestamps (`is timestamp && <= request.time`).

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read/Write**: Caller with `request.auth == null` attempting to read or create a customer.
2. **Cross-Tenant Read**: Authenticated user `alice` reading `/users/bob/customers/cust_1`.
3. **Identity Spoofing on Create**: Authenticated user `alice` writing to `/users/alice/customers/cust_1` with `userId: "bob"`.
4. **Shadow/Ghost Field Injection**: Adding `"isAdmin": true` or `"verified": true` to a Customer or Transaction payload.
5. **Immortal Field Mutation**: Updating `createdAt` or `userId` on an existing Customer or Transaction document.
6. **Negative/Zero Transaction Amount**: Creating a TransactionEntry with `amount: 0` or `amount: -500`.
7. **Invalid Transaction Enum**: Creating a TransactionEntry with `type: "FREE"` instead of `"UDHAR"` or `"JAMA"`.
8. **Unbounded Array Injection**: Passing 25 items in `TransactionEntry.items` (exceeding max 15).
9. **Oversized String / Denial of Wallet**: Passing a 100KB string in `name` or `note`.
10. **Future Timestamp Spoofing**: Passing a timestamp in the future (`createdAt > request.time`).
11. **Value Poisoning on Update**: Updating `netBalance` with a string `"100"` instead of `int`.
12. **Blanket List Scraping**: Listing `/users/bob/transactions` while signed in as `alice`.
