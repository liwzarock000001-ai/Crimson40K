const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_host_uid";
const BOB_UID = "bob_peer_uid";
const EVE_UID = "eve_outsider_uid";
const ROOM_CODE = "482915";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validRoomPayload(uid = ALICE_UID, name = "Commander 1", ts = new Date(Date.now() - 2000)) {
  return {
    roomCode: ROOM_CODE,
    hostUid: uid,
    hostName: name,
    status: "LOBBY",
    openForJoin: true,
    memberUids: [uid],
    spawnAiEnemyIfSameFaction: true,
    players: [
      { id: "p1", name: name, faction: "MARINES", slotMode: "HOST", ready: true, connectedClient: true, uid },
      { id: "p2", name: "Commander 2", faction: "MARINES", slotMode: "LAN_CLIENT", ready: true, connectedClient: false, uid: "" },
    ],
    assignedUnits: [
      { entryId: "c1", ownerPlayerId: "p1", templateId: "captain" },
      { entryId: "c2", ownerPlayerId: "p2", templateId: "intercessors" },
    ],
    createdAt: ts,
    updatedAt: ts,
  };
}

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
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated users cannot read, list, or create coop rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("coop_rooms").doc(ROOM_CODE).set(validRoomPayload()));
  await assertFails(unauthDb.collection("coop_rooms").doc(ROOM_CODE).get());
  await assertFails(unauthDb.collection("coop_rooms").get());
});

test("Host can create room, remote friend can join by Room Code, outsider cannot scrape or hijacking hostUid", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();

  // Create valid room
  await assertSucceeds(aliceDb.collection("coop_rooms").doc(ROOM_CODE).set(validRoomPayload()));

  // Shadow field attack must fail
  await assertFails(
    aliceDb.collection("coop_rooms").doc("999111").set({
      ...validRoomPayload(ALICE_UID),
      roomCode: "999111",
      isAdminOverride: true,
    })
  );

  // Unfiltered list query must fail
  await assertFails(bobDb.collection("coop_rooms").get());

  // Bob joins by knowing the Room Code
  await assertSucceeds(bobDb.collection("coop_rooms").doc(ROOM_CODE).get());
  await assertSucceeds(
    bobDb.collection("coop_rooms").doc(ROOM_CODE).update({
      memberUids: [ALICE_UID, BOB_UID],
      players: [
        { id: "p1", name: "Commander 1", faction: "MARINES", slotMode: "HOST", ready: true, connectedClient: true, uid: ALICE_UID },
        { id: "p2", name: "Bob", faction: "MARINES", slotMode: "LAN_CLIENT", ready: true, connectedClient: true, uid: BOB_UID },
      ],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob queries his own joined rooms
  await assertSucceeds(bobDb.collection("coop_rooms").where("memberUids", "array-contains", BOB_UID).get());

  // Bob (non-host) cannot change room status or hostName
  await assertFails(
    bobDb.collection("coop_rooms").doc(ROOM_CODE).update({
      status: "ACTIVE",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Eve cannot hijack closed/existing room after host sets openForJoin = false
  await assertSucceeds(
    aliceDb.collection("coop_rooms").doc(ROOM_CODE).update({
      openForJoin: false,
      status: "ACTIVE",
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(eveDb.collection("coop_rooms").doc(ROOM_CODE).get());
});

test("Active room members can submit Co-op actions while non-members are denied, and FINISHED locks writes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("coop_rooms").doc(ROOM_CODE).set({
      ...validRoomPayload(),
      status: "ACTIVE",
      openForJoin: false,
      memberUids: [ALICE_UID, BOB_UID],
    });
  });

  // Bob submits a valid command action
  await assertSucceeds(
    bobDb.collection("coop_rooms").doc(ROOM_CODE).collection("actions").doc("act_1").set({
      userId: BOB_UID,
      hostUid: ALICE_UID,
      playerId: "p2",
      actionType: "TAP_WORLD",
      wx: 4.5,
      wy: 6.0,
      createdAt: new Date(Date.now() - 500),
    })
  );

  // Spoofed userId fails
  await assertFails(
    bobDb.collection("coop_rooms").doc(ROOM_CODE).collection("actions").doc("act_2").set({
      userId: ALICE_UID,
      hostUid: ALICE_UID,
      playerId: "p1",
      actionType: "END_TURN",
      createdAt: new Date(Date.now() - 500),
    })
  );

  // Outsider Eve cannot post an action
  await assertFails(
    eveDb.collection("coop_rooms").doc(ROOM_CODE).collection("actions").doc("act_3").set({
      userId: EVE_UID,
      hostUid: ALICE_UID,
      playerId: "p3",
      actionType: "END_TURN",
      createdAt: new Date(Date.now() - 500),
    })
  );

  // Host listens using matching .where("hostUid", "==", ALICE_UID) and cleans up processed action
  await assertSucceeds(
    aliceDb.collection("coop_rooms").doc(ROOM_CODE).collection("actions").where("hostUid", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("coop_rooms").doc(ROOM_CODE).collection("actions").doc("act_1").delete()
  );

  // Terminal state lock: once FINISHED, room cannot be updated
  await assertSucceeds(
    aliceDb.collection("coop_rooms").doc(ROOM_CODE).update({
      status: "FINISHED",
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(
    aliceDb.collection("coop_rooms").doc(ROOM_CODE).update({
      status: "ACTIVE",
      updatedAt: new Date(Date.now() - 50),
    })
  );
});

test("Host can sync customTemplates and non-hosts cannot alter them; FIGHT_FIRST action allowed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const customTemplates = [
    { id: "von_ryans_leaper", name: "Von Ryan's Leaper", points: 85, moveCm: 20, strength: 6 }
  ];

  // Host creates room with customTemplates
  await assertSucceeds(
    aliceDb.collection("coop_rooms").doc(ROOM_CODE).set({
      ...validRoomPayload(),
      customTemplates,
    })
  );

  // Host updates customTemplates in LOBBY
  await assertSucceeds(
    aliceDb.collection("coop_rooms").doc(ROOM_CODE).update({
      customTemplates: [
        { id: "von_ryans_leaper", name: "Von Ryan's Leaper", points: 95, moveCm: 22, strength: 7 }
      ],
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Bob joins, then tries to overwrite customTemplates -> must fail
  await assertSucceeds(
    bobDb.collection("coop_rooms").doc(ROOM_CODE).update({
      memberUids: [ALICE_UID, BOB_UID],
      players: [
        { id: "p1", name: "Commander 1", faction: "MARINES", slotMode: "HOST", ready: true, connectedClient: true, uid: ALICE_UID },
        { id: "p2", name: "Bob", faction: "XENOS", slotMode: "LAN_CLIENT", ready: true, connectedClient: true, uid: BOB_UID },
      ],
      updatedAt: new Date(Date.now() - 300),
    })
  );

  await assertFails(
    bobDb.collection("coop_rooms").doc(ROOM_CODE).update({
      customTemplates: [{ id: "intercessors", strength: 99 }],
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Host starts battle -> Bob submits FIGHT_FIRST action
  await assertSucceeds(
    aliceDb.collection("coop_rooms").doc(ROOM_CODE).update({
      status: "ACTIVE",
      openForJoin: false,
      updatedAt: new Date(Date.now() - 150),
    })
  );

  await assertSucceeds(
    bobDb.collection("coop_rooms").doc(ROOM_CODE).collection("actions").doc("act_ff").set({
      userId: BOB_UID,
      hostUid: ALICE_UID,
      playerId: "p2",
      actionType: "FIGHT_FIRST",
      createdAt: new Date(Date.now() - 100),
    })
  );

  await assertSucceeds(
    bobDb.collection("coop_rooms").doc(ROOM_CODE).collection("actions").doc("act_join").set({
      userId: BOB_UID,
      hostUid: ALICE_UID,
      playerId: "p2",
      actionType: "JOIN_SQUAD",
      wx: 0,
      wy: 1,
      createdAt: new Date(Date.now() - 80),
    })
  );

  await assertSucceeds(
    bobDb.collection("coop_rooms").doc(ROOM_CODE).collection("actions").doc("act_pact").set({
      userId: BOB_UID,
      hostUid: ALICE_UID,
      playerId: "p2",
      actionType: "SELECT_DARK_PACT",
      cm: 1,
      createdAt: new Date(Date.now() - 60),
    })
  );
});

test("Host can set and update pointLimit within 500..5000 while invalid bounds and non-host updates fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Out-of-bounds pointLimit on create fails
  await assertFails(
    aliceDb.collection("coop_rooms").doc(ROOM_CODE).set({
      ...validRoomPayload(),
      pointLimit: 100,
    })
  );

  // Valid pointLimit create succeeds
  await assertSucceeds(
    aliceDb.collection("coop_rooms").doc(ROOM_CODE).set({
      ...validRoomPayload(),
      pointLimit: 1500,
    })
  );

  // Host can update pointLimit
  await assertSucceeds(
    aliceDb.collection("coop_rooms").doc(ROOM_CODE).update({
      pointLimit: 2000,
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Bob joins, then cannot alter pointLimit
  await assertSucceeds(
    bobDb.collection("coop_rooms").doc(ROOM_CODE).update({
      memberUids: [ALICE_UID, BOB_UID],
      players: [
        { id: "p1", name: "Commander 1", faction: "MARINES", slotMode: "HOST", ready: true, connectedClient: true, uid: ALICE_UID },
        { id: "p2", name: "Bob", faction: "MARINES", slotMode: "LAN_CLIENT", ready: true, connectedClient: true, uid: BOB_UID },
      ],
      updatedAt: new Date(Date.now() - 200),
    })
  );

  await assertFails(
    bobDb.collection("coop_rooms").doc(ROOM_CODE).update({
      pointLimit: 5000,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});


