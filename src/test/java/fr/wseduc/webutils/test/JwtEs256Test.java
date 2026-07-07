package fr.wseduc.webutils.test;

import fr.wseduc.webutils.security.JWT;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.Assert.*;

public class JwtEs256Test {

	/**
	 * Test that a valid ES256 JWT token can be verified.
	 */
	@Test
	public void testVerifyAndGet_ES256_roundTrip() throws Exception {
		KeyPair keyPair = generateECKeyPair();

		String token = createES256Token(
				new JsonObject().put("sub", "test-user").put("name", "Test"),
				keyPair.getPrivate()
		);

		JsonObject payload = JWT.verifyAndGet(token, keyPair.getPublic());

		assertNotNull("Payload should not be null after successful verification", payload);
		assertEquals("test-user", payload.getString("sub"));
		assertEquals("Test", payload.getString("name"));
	}

	/**
	 * Test that verification fails with a wrong public key.
	 */
	@Test
	public void testVerifyAndGet_ES256_wrongKey() throws Exception {
		KeyPair signingKeyPair = generateECKeyPair();
		KeyPair otherKeyPair = generateECKeyPair();

		String token = createES256Token(
				new JsonObject().put("sub", "user1"),
				signingKeyPair.getPrivate()
		);

		JsonObject payload = JWT.verifyAndGet(token, otherKeyPair.getPublic());
		assertNull("Payload should be null when verified with wrong key", payload);
	}

	/**
	 * Test that a tampered token fails verification.
	 */
	@Test
	public void testVerifyAndGet_ES256_tamperedPayload() throws Exception {
		KeyPair keyPair = generateECKeyPair();

		String token = createES256Token(
				new JsonObject().put("sub", "original"),
				keyPair.getPrivate()
		);

		// Tamper with the payload
		String[] parts = token.split("\\.");
		JsonObject tamperedPayload = new JsonObject().put("sub", "hacker");
		String tamperedPart = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(tamperedPayload.encode().getBytes("UTF-8"));
		String tamperedToken = parts[0] + "." + tamperedPart + "." + parts[2];

		JsonObject result = JWT.verifyAndGet(tamperedToken, keyPair.getPublic());
		assertNull("Tampered token should not verify", result);
	}

	/**
	 * Test verification of a token with a complete payload (iss, sub, aud, iat, exp).
	 */
	@Test
	public void testVerifyAndGet_ES256_fullPayload() throws Exception {
		KeyPair keyPair = generateECKeyPair();

		JsonObject expectedPayload = new JsonObject()
				.put("iss", "test-issuer")
				.put("sub", "user-123")
				.put("aud", "my-app")
				.put("iat", 1700000000)
				.put("exp", 1700003600);

		String token = createES256Token(expectedPayload, keyPair.getPrivate());

		JsonObject result = JWT.verifyAndGet(token, keyPair.getPublic());

		assertNotNull(result);
		assertEquals("test-issuer", result.getString("iss"));
		assertEquals("user-123", result.getString("sub"));
		assertEquals("my-app", result.getString("aud"));
		assertEquals(Integer.valueOf(1700000000), result.getInteger("iat"));
		assertEquals(Integer.valueOf(1700003600), result.getInteger("exp"));
	}

	/**
	 * Test that verifyAndGet returns null for a null public key.
	 */
	@Test
	public void testVerifyAndGet_ES256_nullKey() throws Exception {
		KeyPair keyPair = generateECKeyPair();

		String token = createES256Token(
				new JsonObject().put("sub", "user"),
				keyPair.getPrivate()
		);

		JsonObject result = JWT.verifyAndGet(token, (PublicKey) null);
		assertNull("Should return null for null public key", result);
	}

	/**
	 * Test that verifyAndGet returns null for a malformed token.
	 */
	@Test
	public void testVerifyAndGet_ES256_malformedToken() throws Exception {
		KeyPair keyPair = generateECKeyPair();

		JsonObject result = JWT.verifyAndGet("not.a.valid-token", keyPair.getPublic());
		assertNull("Should return null for malformed token", result);
	}

	/**
	 * Test that verifyAndGet returns null for a token with only 2 parts.
	 */
	@Test
	public void testVerifyAndGet_ES256_incompleteParts() throws Exception {
		KeyPair keyPair = generateECKeyPair();

		JsonObject result = JWT.verifyAndGet("part1.part2", keyPair.getPublic());
		assertNull("Should return null for token with fewer than 3 parts", result);
	}

	/**
	 * Test full round-trip: sign with Java (DER), convert to JWS raw format,
	 * then verify via JWT.verifyAndGet which internally converts back to DER.
	 */
	@Test
	public void testVerifyAndGet_ES256_multipleSignatures() throws Exception {
		KeyPair keyPair = generateECKeyPair();

		// Sign multiple tokens to exercise different random signature values
		// (ECDSA signatures are non-deterministic, so r/s values will vary)
		for (int i = 0; i < 20; i++) {
			JsonObject payload = new JsonObject().put("iteration", i).put("data", "test-" + i);
			String token = createES256Token(payload, keyPair.getPrivate());

			JsonObject result = JWT.verifyAndGet(token, keyPair.getPublic());

			assertNotNull("Iteration " + i + ": payload should not be null", result);
			assertEquals("Iteration " + i, i, result.getInteger("iteration").intValue());
		}
	}

	// ===== Helper methods =====

	private static KeyPair generateECKeyPair() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
		keyGen.initialize(new ECGenParameterSpec("secp256r1"));
		return keyGen.generateKeyPair();
	}

	/**
	 * Creates an ES256-signed JWT token.
	 */
	private String createES256Token(JsonObject payload, PrivateKey privateKey) throws Exception {
		JsonObject header = new JsonObject()
				.put("typ", "JWT")
				.put("alg", "ES256")
				.put("kid", "test-key-1");

		String headerB64 = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(header.encode().getBytes("UTF-8"));
		String payloadB64 = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(payload.encode().getBytes("UTF-8"));

		String signingInput = headerB64 + "." + payloadB64;

		// Sign with SHA256withECDSA (produces DER format)
		Signature signer = Signature.getInstance("SHA256withECDSA");
		signer.initSign(privateKey);
		signer.update(signingInput.getBytes("UTF-8"));
		byte[] derSignature = signer.sign();

		// Convert DER to JWS raw format (R || S)
		byte[] rawSignature = convertDERToJWS(derSignature);

		String signatureB64 = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(rawSignature);

		return signingInput + "." + signatureB64;
	}

	/**
	 * Converts a DER-encoded ECDSA signature to the JWS raw format (R || S, 32 bytes each).
	 */
	private static byte[] convertDERToJWS(byte[] derSignature) {
		// DER format: 0x30 [len] 0x02 [r-len] [r] 0x02 [s-len] [s]
		int offset = 2; // skip SEQUENCE tag and length

		// Read r
		if (derSignature[offset] != 0x02) throw new IllegalArgumentException("Expected INTEGER tag for r");
		offset++;
		int rLen = derSignature[offset++] & 0xFF;
		byte[] r = Arrays.copyOfRange(derSignature, offset, offset + rLen);
		offset += rLen;

		// Read s
		if (derSignature[offset] != 0x02) throw new IllegalArgumentException("Expected INTEGER tag for s");
		offset++;
		int sLen = derSignature[offset++] & 0xFF;
		byte[] s = Arrays.copyOfRange(derSignature, offset, offset + sLen);

		// Pad or trim to exactly 32 bytes each
		byte[] result = new byte[64];
		copyWithPadding(r, result, 0);
		copyWithPadding(s, result, 32);
		return result;
	}

	/**
	 * Copies a big-endian integer value into a fixed 32-byte slot, handling leading zeros.
	 */
	private static void copyWithPadding(byte[] src, byte[] dest, int destOffset) {
		if (src.length == 33 && src[0] == 0) {
			// Remove leading zero padding
			System.arraycopy(src, 1, dest, destOffset, 32);
		} else if (src.length == 32) {
			System.arraycopy(src, 0, dest, destOffset, 32);
		} else if (src.length < 32) {
			// Left-pad with zeros
			System.arraycopy(src, 0, dest, destOffset + (32 - src.length), src.length);
		} else {
			throw new IllegalArgumentException("Unexpected integer length: " + src.length);
		}
	}
}


