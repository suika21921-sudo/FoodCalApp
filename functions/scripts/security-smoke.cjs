const assert = require("node:assert/strict");
const {
  isPublicIpAddress,
  parseAIAssistInput,
} = require("../lib/security.js");

assert.deepEqual(parseAIAssistInput({ itemId: "abc_123", bias: "critical" }), {
  itemId: "abc_123",
  bias: "critical",
});
assert.equal(parseAIAssistInput({ itemId: "../admin", bias: "neutral" }), null);
assert.equal(parseAIAssistInput({ itemId: "abc", bias: "anything" }), null);
assert.equal(parseAIAssistInput(null), null);

for (const address of [
  "0.0.0.0",
  "10.0.0.1",
  "100.64.0.1",
  "127.0.0.1",
  "169.254.169.254",
  "172.16.0.1",
  "192.168.1.1",
  "198.18.0.1",
  "224.0.0.1",
  "::",
  "::1",
  "fc00::1",
  "fe80::1",
  "ff02::1",
  "2001:db8::1",
  "64:ff9b::7f00:1",
  "2002:7f00:1::",
  "fec0::1",
  "::ffff:127.0.0.1",
]) {
  assert.equal(isPublicIpAddress(address), false, `${address} must be rejected`);
}

for (const address of ["1.1.1.1", "8.8.8.8", "2606:4700:4700::1111"]) {
  assert.equal(isPublicIpAddress(address), true, `${address} must be accepted`);
}

console.log("Security helper smoke tests passed");
