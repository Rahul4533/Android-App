const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_shop_123";
const BOB_UID = "bob_shop_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or create customers", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("customers").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1").set({
      userId: ALICE_UID,
      name: "Ramesh Kumar",
      phone: "9876543210",
      photoBase64: "",
      netBalance: 120,
      totalUdhar: 120,
      totalJama: 0,
      lastItemsSummary: "Milk, Bread",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can create, read, and update own customer", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const custRef = aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1");

  await assertSucceeds(
    custRef.set({
      userId: ALICE_UID,
      name: "Ramesh Kumar",
      phone: "9876543210",
      photoBase64: "",
      netBalance: 150,
      totalUdhar: 150,
      totalJama: 0,
      lastItemsSummary: "Milk, Biscuit",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(custRef.get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customers").get()
  );

  // Update balance
  await assertSucceeds(
    custRef.update({
      netBalance: 50,
      totalJama: 100,
      updatedAt: new Date(),
    })
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's customer or transactions", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1").set({
      userId: ALICE_UID,
      name: "Ramesh Kumar",
      phone: "9876543210",
      photoBase64: "",
      netBalance: 150,
      totalUdhar: 150,
      totalJama: 0,
      lastItemsSummary: "Milk, Biscuit",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      customerId: "cust_1",
      customerName: "Ramesh Kumar",
      type: "UDHAR",
      amount: 150,
      items: ["Milk", "Biscuit"],
      note: "Morning",
      photoBase64: "",
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust_1").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("customers").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
});

test("Shadow update & invalid schema rejection on transactions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_valid");

  // Valid create succeeds
  await assertSucceeds(
    txRef.set({
      userId: ALICE_UID,
      customerId: "cust_1",
      customerName: "Suresh",
      type: "UDHAR",
      amount: 85,
      items: ["Bread", "Oil"],
      note: "1L oil + bread",
      photoBase64: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Ghost field fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_ghost").set({
      userId: ALICE_UID,
      customerId: "cust_1",
      customerName: "Suresh",
      type: "UDHAR",
      amount: 85,
      items: ["Bread"],
      note: "",
      photoBase64: "",
      isVerified: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Invalid amount (0 or negative) fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_zero").set({
      userId: ALICE_UID,
      customerId: "cust_1",
      customerName: "Suresh",
      type: "UDHAR",
      amount: 0,
      items: ["Bread"],
      note: "",
      photoBase64: "",
      createdAt: now,
      updatedAt: now,
    })
  );
});
