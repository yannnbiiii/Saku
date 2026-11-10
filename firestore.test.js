const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

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

test("Unauthenticated user cannot read or write expenses", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/expenses/exp1`).get());
  await assertFails(
    unauthDb.doc(`users/${ALICE_UID}/expenses/exp1`).set({
      userId: ALICE_UID,
      title: "Makan Siang",
      amount: 25000,
      categoryId: "cat_food",
      categoryName: "Makanan",
      date: "2026-11-08",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Alice can create and read her own expense", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/expenses/exp1`).set({
      userId: ALICE_UID,
      title: "Makan Siang",
      amount: 25000,
      categoryId: "cat_food",
      categoryName: "Makanan",
      date: "2026-11-08",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/expenses/exp1`).get());
});

test("Bob cannot read or modify Alice's expense", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().doc(`users/${ALICE_UID}/expenses/exp1`).set({
      userId: ALICE_UID,
      title: "Makan Siang",
      amount: 25000,
      categoryId: "cat_food",
      categoryName: "Makanan",
      date: "2026-11-08",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}/expenses/exp1`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/expenses/exp1`).update({
      amount: 50000,
      updatedAt: new Date(),
    })
  );
});

test("Validation rejects expense with invalid amount or empty title", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/expenses/exp_bad1`).set({
      userId: ALICE_UID,
      title: "",
      amount: 25000,
      categoryId: "cat_food",
      categoryName: "Makanan",
      date: "2026-11-08",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/expenses/exp_bad2`).set({
      userId: ALICE_UID,
      title: "Makan Siang",
      amount: 0,
      categoryId: "cat_food",
      categoryName: "Makanan",
      date: "2026-11-08",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Alice can create and read category and budget", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/categories/cat1`).set({
      userId: ALICE_UID,
      name: "Makanan",
      iconName: "restaurant",
      colorHex: "#10B981",
      isDefault: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/budgets/bud1`).set({
      userId: ALICE_UID,
      categoryId: "cat1",
      categoryName: "Makanan",
      monthlyLimit: 1000000,
      month: "2026-11",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});
