const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated user cannot read or write user profiles, conversations, or messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("conversations").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_1")
      .set({
        userId: ALICE_UID,
        title: "Hack",
        agentMode: "GENERAL_ASSISTANT",
        lastMessagePreview: "Hi",
        messageCount: 1,
        createdAt: now,
        updatedAt: now,
      })
  );
});

test("Owner can create and update their UserProfile; cross-user and ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Valid Kotlin-mirrored profile create (no null optional keys, no @DocumentId id)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      preferredMode: "GENERAL_ASSISTANT",
      hinglishPreferred: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update on allowed fields succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredMode: "DATA_ANALYST",
      hinglishPreferred: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot read or write Alice's profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Immutable createdAt cannot be modified
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Shadow update with ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Owner can create, list, update, and delete conversations and messages; cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  const convRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("conversations")
    .doc("conv_101");

  // Batch create conversation + first user message (mirrors Kotlin WriteBatch)
  const batch = aliceDb.batch();
  batch.set(convRef, {
    userId: ALICE_UID,
    title: "Analyze sales dataset",
    agentMode: "DATA_ANALYST",
    lastMessagePreview: "Analyze Q4 revenue trends",
    messageCount: 1,
    createdAt: created,
    updatedAt: created,
  });
  batch.set(convRef.collection("messages").doc("msg_1"), {
    userId: ALICE_UID,
    conversationId: "conv_101",
    role: "user",
    text: "Analyze Q4 revenue trends",
    agentMode: "DATA_ANALYST",
    thinkingSteps: [],
    followUpSuggestions: [],
    attachmentLabel: "q4_sales.csv",
    createdAt: created,
  });
  await assertSucceeds(batch.commit());

  // Add model response with thinkingSteps and followUpSuggestions
  await assertSucceeds(
    convRef.collection("messages").doc("msg_2").set({
      userId: ALICE_UID,
      conversationId: "conv_101",
      role: "model",
      text: "Here is your Q4 breakdown.",
      agentMode: "DATA_ANALYST",
      thinkingSteps: ["Understanding request", "Analyzing metrics"],
      followUpSuggestions: ["Compare with Q3", "Forecast Q1"],
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // List queries matching Kotlin repository
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .orderBy("updatedAt", "desc")
      .get()
  );
  await assertSucceeds(
    convRef.collection("messages").orderBy("createdAt", "asc").get()
  );

  // Reject @DocumentId 'id' inside payload
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_bad_id")
      .set({
        id: "conv_bad_id",
        userId: ALICE_UID,
        title: "Bad ID payload",
        agentMode: "CODER",
        lastMessagePreview: "hi",
        messageCount: 0,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject tampered future timestamp
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_future")
      .set({
        userId: ALICE_UID,
        title: "Future",
        agentMode: "CODER",
        lastMessagePreview: "hi",
        messageCount: 0,
        createdAt: new Date(Date.now() + 60000),
        updatedAt: new Date(Date.now() + 60000),
      })
  );

  // Cross-user read/list/delete rejected
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("conversations").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_101")
      .collection("messages")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_101")
      .delete()
  );

  // Owner partial update and delete succeed
  await assertSucceeds(
    convRef.update({
      title: "Q4 Revenue Insights",
      lastMessagePreview: "Here is your Q4 breakdown.",
      messageCount: 2,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(convRef.collection("messages").doc("msg_1").delete());
  await assertSucceeds(convRef.delete());
});
