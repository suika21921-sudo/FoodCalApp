const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} = require("@firebase/rules-unit-testing");
const {
  collection,
  doc,
  getDoc,
  getDocs,
  setDoc,
  updateDoc,
} = require("firebase/firestore");
const {
  getBytes,
  ref,
  uploadBytes,
} = require("firebase/storage");

const projectId = "mealoo-rules-test";
const repositoryRoot = path.resolve(__dirname, "../..");
let testEnvironment;

describe("Firebase security rules", () => {
  before(async () => {
    testEnvironment = await initializeTestEnvironment({
      projectId,
      firestore: {
        rules: fs.readFileSync(path.join(repositoryRoot, "firestore.rules"), "utf8"),
      },
      storage: {
        rules: fs.readFileSync(path.join(repositoryRoot, "storage.rules"), "utf8"),
      },
    });
  });

  beforeEach(async () => {
    await testEnvironment.clearFirestore();
    await testEnvironment.clearStorage();
    await testEnvironment.withSecurityRulesDisabled(async (context) => {
      const database = context.firestore();
      await setDoc(doc(database, "community/recipes/shared/recipe-1"), {
        name: "Public recipe",
        creatorId: "author-1",
      });
      await setDoc(doc(database, "public/info/items/item-1"), {
        title: "Public article",
        url: "https://example.com/article",
        aiAssist: { neutral: "server generated" },
      });
      await setDoc(doc(database, "_functionRateLimits/generateAIAssist_user-1"), {
        minuteCount: 1,
        dayCount: 1,
      });
      await uploadBytes(ref(context.storage(), "existing/test.txt"), new Uint8Array([4, 5, 6]), {
        contentType: "text/plain",
      });
    });
  });

  after(async () => {
    await testEnvironment.cleanup();
  });

  it("allows public reads of shared community recipes", async () => {
    const database = testEnvironment.unauthenticatedContext().firestore();
    await assertSucceeds(getDoc(doc(database, "community/recipes/shared/recipe-1")));
    const snapshot = await assertSucceeds(
      getDocs(collection(database, "community/recipes/shared"))
    );
    assert.equal(snapshot.size, 1);
  });

  it("denies all client writes to shared community recipes", async () => {
    const anonymousDatabase = testEnvironment.unauthenticatedContext().firestore();
    const authenticatedDatabase = testEnvironment.authenticatedContext("user-1").firestore();

    await assertFails(
      setDoc(doc(anonymousDatabase, "community/recipes/shared/recipe-2"), { name: "Injected" })
    );
    await assertFails(
      updateDoc(doc(authenticatedDatabase, "community/recipes/shared/recipe-1"), {
        name: "Tampered",
      })
    );
  });

  it("allows public reads but denies client writes to public item documents", async () => {
    const anonymousDatabase = testEnvironment.unauthenticatedContext().firestore();
    const authenticatedDatabase = testEnvironment.authenticatedContext("user-1").firestore();

    await assertSucceeds(getDoc(doc(anonymousDatabase, "public/info/items/item-1")));
    await assertFails(
      updateDoc(doc(authenticatedDatabase, "public/info/items/item-1"), {
        "aiAssist.neutral": "client supplied",
      })
    );
    await assertFails(
      updateDoc(doc(authenticatedDatabase, "public/info/items/item-1"), {
        url: "https://attacker.example/article",
      })
    );
  });

  it("denies all client access to function rate-limit documents", async () => {
    const database = testEnvironment.authenticatedContext("user-1").firestore();
    const rateLimit = doc(database, "_functionRateLimits/generateAIAssist_user-1");

    await assertFails(getDoc(rateLimit));
    await assertFails(updateDoc(rateLimit, { dayCount: 0 }));
    await assertFails(setDoc(doc(database, "_functionRateLimits/custom"), { dayCount: 0 }));
  });

  it("denies unmatched Firestore paths by default", async () => {
    const database = testEnvironment.authenticatedContext("user-1").firestore();

    await assertFails(getDoc(doc(database, "unconfigured/documents/example/item")));
    await assertFails(
      setDoc(doc(database, "unconfigured/documents/example/item"), { value: true })
    );
  });

  it("denies all Firebase Storage reads and writes while Storage is unused", async () => {
    const anonymousStorage = testEnvironment.unauthenticatedContext().storage();
    const authenticatedStorage = testEnvironment.authenticatedContext("user-1").storage();
    const data = new Uint8Array([1, 2, 3]);

    await assertFails(uploadBytes(ref(anonymousStorage, "public/test.txt"), data, {
      contentType: "text/plain",
    }));
    await assertFails(uploadBytes(ref(authenticatedStorage, "users/user-1/test.txt"), data, {
      contentType: "text/plain",
    }));
    await assertFails(getBytes(ref(anonymousStorage, "existing/test.txt")));
    await assertFails(getBytes(ref(authenticatedStorage, "existing/test.txt")));
  });
});
