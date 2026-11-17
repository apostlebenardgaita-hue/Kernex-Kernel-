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

function buildValidProfile(uid, ts) {
  return {
    userId: uid,
    name: "Kernix Workstation",
    baseDistro: "Arch-Minimal",
    architecture: "x86_64",
    initSystem: "systemd",
    filesystem: "btrfs",
    kernelVersion: "6.12 LTS",
    scheduler: "BORE",
    preemption: "PREEMPT_DYNAMIC",
    compiler: "Clang ThinLTO",
    kernelFlags: ["CONFIG_WIREGUARD", "CONFIG_BPF_JIT", "CONFIG_ZRAM"],
    desktopEnv: "Hyprland",
    statusBar: "Waybar",
    barPosition: "Top",
    terminal: "Ghostty",
    launcher: "Rofi",
    accentPreset: "Cyber Cyan",
    windowGaps: 10,
    borderWidth: 2,
    cornerRadius: 12,
    blurStrength: 6,
    windowOpacity: 0.92,
    animationStyle: "Snappy Spring",
    keybindStyle: "Vim-like",
    createdAt: ts,
    updatedAt: ts,
  };
}

function buildValidLanguage(uid, ts) {
  return {
    userId: uid,
    name: "KoraLang",
    fileExtension: ".kx",
    commentPrefix: "//",
    kwVar: "make",
    kwPrint: "say",
    kwFunc: "task",
    kwIf: "when",
    kwElse: "otherwise",
    kwLoop: "repeat",
    kwReturn: "give",
    kwTrue: "yes",
    kwFalse: "no",
    createdAt: ts,
    updatedAt: ts,
  };
}

function buildValidScript(uid, ts) {
  return {
    userId: uid,
    title: "Kernel Check",
    languageId: "lang_1",
    sourceCode: 'make x = 42\nsay "Kernel ready: " + x',
    lastOutput: "Kernel ready: 42",
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated user cannot read or write any Kernix collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 1000);
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("linuxProfiles").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("linuxProfiles").doc("p1").set(buildValidProfile(ALICE_UID, ts)));
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("languages").doc("l1").set(buildValidLanguage(ALICE_UID, ts)));
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("scripts").doc("s1").set(buildValidScript(ALICE_UID, ts)));
});

test("Owner can create, list, update, and delete LinuxProfile", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const col = alice.collection("users").doc(ALICE_UID).collection("linuxProfiles");

  await assertSucceeds(col.doc("prof_1").set(buildValidProfile(ALICE_UID, created)));
  await assertSucceeds(col.get());
  await assertSucceeds(
    col.doc("prof_1").update({
      name: "Kernix Gaming Rig",
      windowGaps: 14,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field / @DocumentId id field / immutable createdAt change / out-of-range gaps
  await assertFails(col.doc("prof_2").set({ ...buildValidProfile(ALICE_UID, created), id: "prof_2" }));
  await assertFails(col.doc("prof_1").update({ createdAt: new Date(Date.now() - 500), updatedAt: new Date(Date.now() - 500) }));
  await assertFails(col.doc("prof_1").update({ windowGaps: 99, updatedAt: new Date(Date.now() - 500) }));
  await assertFails(col.doc("prof_1").update({ isAdmin: true, updatedAt: new Date(Date.now() - 500) }));

  await assertSucceeds(col.doc("prof_1").delete());
});

test("Owner can create, update, and list CustomLanguage and CodeScript", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const langCol = alice.collection("users").doc(ALICE_UID).collection("languages");
  const scriptCol = alice.collection("users").doc(ALICE_UID).collection("scripts");

  await assertSucceeds(langCol.doc("lang_1").set(buildValidLanguage(ALICE_UID, created)));
  await assertSucceeds(langCol.get());
  await assertSucceeds(
    langCol.doc("lang_1").update({
      kwPrint: "show",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // Reject invalid keyword with spaces
  await assertFails(langCol.doc("lang_1").update({ kwPrint: "bad keyword", updatedAt: new Date(Date.now() - 500) }));

  await assertSucceeds(scriptCol.doc("script_1").set(buildValidScript(ALICE_UID, created)));
  await assertSucceeds(scriptCol.get());
  await assertSucceeds(
    scriptCol.doc("script_1").update({
      lastOutput: "Updated output",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // Reject future tampered timestamp
  await assertFails(scriptCol.doc("script_2").set(buildValidScript(ALICE_UID, new Date(Date.now() + 120000))));
});

test("Cross-user isolation: Bob cannot read or mutate Alice's documents", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("linuxProfiles").doc("p1").set(buildValidProfile(ALICE_UID, ts)));
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("languages").doc("l1").set(buildValidLanguage(ALICE_UID, ts)));
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("scripts").doc("s1").set(buildValidScript(ALICE_UID, ts)));

  await assertFails(bob.collection("users").doc(ALICE_UID).collection("linuxProfiles").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("linuxProfiles").doc("p1").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("linuxProfiles").doc("p1").update({ name: "Hacked", updatedAt: ts }));
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("languages").doc("l1").delete());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("scripts").doc("s1").get());
});
