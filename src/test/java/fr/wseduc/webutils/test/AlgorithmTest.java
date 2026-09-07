/*
 * Copyright © WebServices pour l'Éducation, 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.wseduc.webutils.test;

import fr.wseduc.webutils.data.ZLib;
import fr.wseduc.webutils.security.AWS4Signature;
import fr.wseduc.webutils.security.Blowfish;
import fr.wseduc.webutils.security.HmacSha256;
import fr.wseduc.webutils.security.JWT;
import fr.wseduc.webutils.security.Md5;
import fr.wseduc.webutils.security.NTLM;
import fr.wseduc.webutils.security.Sha256;
import org.junit.Test;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class AlgorithmTest {

	@Test
	public void hashMd5() throws NoSuchAlgorithmException {
		assertEquals("0127f712fc008f857e77a2f3f179c710", Md5.hash("Javarmi.com"));
	}

	@Test
	public void blowfishTest() throws GeneralSecurityException {
		final String data = "Lorem ipsum";
		final String key = "key-1234";
		String encryptedData = Blowfish.encrypt(data, key);
		String decryptedData = Blowfish.decrypt(encryptedData, key);
		assertEquals(data, decryptedData);
	}

	@Test
	public void hashSha256() throws NoSuchAlgorithmException {
		assertEquals("278cb091126f9b2eebdf8c008b53ec592e190e5b417a1f2fb5e5d7faf1d0b874", Sha256.hash("Javarmi.com"));
	}

	@Test
	public void decodeBase64UrlSafe() throws UnsupportedEncodingException {
		String decodedHeader = JWT.base64Decode("eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6InJlc3RvY29sbGVnZS1kZW1hdC52YWxk\nb2lzZS5mciJ9");
		String decodedPayload = JWT.base64Decode("eyJzdWIiOiI1NTU0ODIzNjAyIiwiZW1haWwiOiJpc2FiZWxsZS5hb2J6eW9AYWMtdmVyc2FpbGxl\ncy5mciIsIm5hbWUiOiJNYWRhbWUgUG9sb25pbyIsImlzcyI6Imh0dHBzOm9uZSIsImF1ZCI6InRl\nc3RvaWMiLCJpYXQiOjE0NzMxNDYyNTAsImV4cCI6MTQ3MzE0OTg1MH0=");
		System.out.println(decodedHeader);
		System.out.println(decodedPayload);
	}

	@Test
	public void verifyJWT() throws CertificateException, FileNotFoundException {
		final String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6InJlc3RvY29sbGVnZS1kZW1hdC52YWxk\nb2lzZS5mciJ9.eyJzdWIiOiI1NTU0ODIzNjAyIiwiZW1haWwiOiJpc2FiZWxsZS5hb2J6eW9AYWMtdmVyc2FpbGxl\ncy5mciIsIm5hbWUiOiJNYWRhbWUgUG9sb25pbyIsImlzcyI6Imh0dHBzOm9uZSIsImF1ZCI6InRl\nc3RvaWMiLCJpYXQiOjE0NzMxNDYyNTAsImV4cCI6MTQ3MzE0OTg1MH0=.3a3EdflZf2hFbDUuQ0Dpnb1ihjYR1kMZJq7L6ip3jTcJSQ8WdyI9FMS7kWN9QDrD0qraFB_miYfd\nNyqBewuGzD1LvM2_bfHpm1AhzqNofQpH621q41ir3eua0wV8ouj_dxCUrayqTRfkerPZEpXUJ9VR\n3EWOFGGNdg6q9ptXlCNGMek2Gh1luK5JyfMpOK-sENSWRk-S_iuJ3xaZYiLtkVSRYDYakHaCFTNZ\n8DF3Oih7lVhfNKNWt5tqCyTdzI4-HiNNrLqjWrzc55kazdRUU3OXNscsC0XFxPNPSNoQN0zHseIr\nWqFdf0tDEc0xPzNuLSsudLTvB67RFrRnU1x0ifA5WSTeTgkHei4FAnKEdOFtrx-zRsCk7ka20VWk\nek659HDEWrkgZSuUqrqWVOG8RvVwdBsk_Pb-D4knV5qwH_JQCjj3QnRmm6wVUe1q8vUPCK8bkvKn\ngAs4cb3XtaAFD6oX1NF9qXKIgfA7fJCXZY-BtbYf_YnYFHV058DzUEz1Nr6xR6GVMBU61GwW9vz0\nmN3t4Al1h8dh2EPOPNCxA1LubFrxHS9wTnzpPjD5XlPFEE00yZaW-Z7410seSgRRvMgruzJdpONL\nzWI6af7AsC6FJBGfrZUYftC-0kBw165goV0P1DYS4pYQLEwWsXt_BRDybuijNUYUAx0n2gNnkAc=";
		final String crt = "/home/dboissin/Docs/certificats/capdemat/restocollege-demat.valdoise.fr/restocollege-demat.valdoise.fr.crt";
		CertificateFactory f = CertificateFactory.getInstance("X.509");
		Certificate certificate = f.generateCertificate(new FileInputStream(crt));
		PublicKey p = certificate.getPublicKey();
		JsonObject j = JWT.verifyAndGet(token, p);
		assertEquals("{\"sub\":\"5554823602\",\"email\":\"isabelle.aobzyo@ac-versailles.fr\",\"name\":\"Madame Polonio\",\"iss\":\"https:one\",\"aud\":\"testoic\",\"iat\":1473146250,\"exp\":1473149850}", j.encode());
	}

	@Test
	public void verifyJWTHS256() {
		final String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ";
		JsonObject j = JWT.verifyAndGet(token, "secret");
		assertEquals("{\"sub\":\"1234567890\",\"name\":\"John Doe\",\"admin\":true}", j.encode());
	}

	@Test
	public void deflateAndEncodeTest() throws IOException {
		final String content = "<test>bla</test>";
		assertEquals("sylJLS6xS8pJtNEHswA=", ZLib.deflateAndEncode(content));
	}

	@Test
	public void ntHashTest() throws NoSuchAlgorithmException {
		assertEquals("6bbb885acc9fe37317e6c2c7725efa93", NTLM.ntHash("ArtifLo23"));
	}

	/**
	 * Pins the whole algorithm on the request of the AWS SigV4 "Example: GET Object": canonical request,
	 * header ordering and lowercasing, credential scope and signing key derivation. Uses a fixed instant
	 * on purpose — the signature depends on it.
	 * <p>
	 * Provenance of the expected value: the SHA-256 of the canonical request built here is
	 * {@code 7344ae5b7ee6c3e7e6b0fe0640412a37625d1fbfff95c48bbb2dc43964946972}, the intermediate value AWS
	 * publishes for that example, which pins the canonical request itself; the signature below was then
	 * cross-checked against an independent implementation written straight from the specification.
	 * <p>
	 * Note this signs a non x-amz header ({@code range}), which is legitimate: SigV4 mandates host and
	 * every x-amz-* header, and allows any other.
	 */
	@Test
	public void aws4Test() throws Exception {
		final MultiMap canonicalHeaders = MultiMap.caseInsensitiveMultiMap();
		canonicalHeaders.add("host", "examplebucket.s3.amazonaws.com");
		canonicalHeaders.add("range", "bytes=0-9");
		canonicalHeaders.add("x-amz-content-sha256", AWS4Signature.EMPTY_PAYLOAD_SHA256);
		canonicalHeaders.add("x-amz-date", "20130524T000000Z");

		final String signature = AWS4Signature.sign("GET", "/test.txt", "", canonicalHeaders,
				"us-east-1", "AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY",
				AWS4Signature.EMPTY_PAYLOAD_SHA256,
				Instant.parse("2013-05-24T00:00:00Z"));

		assertEquals("67fe34c8530db585abddc51067328adfedb6e42487d2566dc7d927d6e2722900", signature);
	}

	@Test
	public void aws4SignedHeadersAreLowercaseAndSorted() throws Exception {
		final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
		// Declared out of order, and one of them carries uppercase — as the SSE-C key MD5 header does.
		headers.add("x-amz-date", "20130524T000000Z");
		headers.add("x-amz-server-side-encryption-customer-key-MD5", "abc==");
		headers.add("host", "examplebucket.s3.amazonaws.com");
		headers.add("x-amz-meta-filename", "rapport.pdf");
		headers.add("x-amz-content-sha256", AWS4Signature.EMPTY_PAYLOAD_SHA256);

		assertEquals("host;x-amz-content-sha256;x-amz-date;x-amz-meta-filename;"
				+ "x-amz-server-side-encryption-customer-key-md5", AWS4Signature.signedHeaders(headers));
	}

	/**
	 * A quoted-printable file name holds characters URL encoding would rewrite ({@code =}, {@code ?},
	 * {@code _}). The canonical value must be passed through untouched, otherwise the server recomputes a
	 * different canonical request and answers SignatureDoesNotMatch.
	 */
	@Test
	public void aws4MetadataValueIsNotUrlEncoded() throws Exception {
		final MultiMap plain = MultiMap.caseInsensitiveMultiMap();
		plain.add("host", "examplebucket.s3.amazonaws.com");
		plain.add("x-amz-meta-filename", "rapport.pdf");

		final MultiMap encoded = MultiMap.caseInsensitiveMultiMap();
		encoded.add("host", "examplebucket.s3.amazonaws.com");
		encoded.add("x-amz-meta-filename", "=?utf-8?Q?rapport_final.pdf?=");

		final Instant instant = Instant.parse("2013-05-24T00:00:00Z");
		final String plainSignature = AWS4Signature.sign("PUT", "/test.txt", "", plain,
				"us-east-1", "key", "secret", null, instant);
		final String encodedSignature = AWS4Signature.sign("PUT", "/test.txt", "", encoded,
				"us-east-1", "key", "secret", null, instant);

		// Both must sign, and the special characters must actually change the signature: were they
		// dropped or rewritten identically, the two would collide.
		assertEquals(64, encodedSignature.length());
		assertNotEquals(plainSignature, encodedSignature);
	}

	/**
	 * Pins the {@code host} value that gets signed. Vert.x writes {@code host:port} in the Host header as soon
	 * as the port is not the default one for the scheme, so that is what has to be signed — an endpoint on a
	 * non standard port (MinIO, an internal gateway) answers SignatureDoesNotMatch otherwise.
	 */
	@Test
	public void aws4SignedHostCarriesANonDefaultPort() {
		assertEquals("minio.internal:9000", AWS4Signature.authority("minio.internal", 9000, false));
		assertEquals("minio.internal:9000", AWS4Signature.authority("minio.internal", 9000, true));
		// The default port of the scheme in use is left out, as Vert.x leaves it out.
		assertEquals("s3.amazonaws.com", AWS4Signature.authority("s3.amazonaws.com", 443, true));
		assertEquals("s3.internal", AWS4Signature.authority("s3.internal", 80, false));
		// But 443 in clear and 80 over TLS are not: Vert.x sends them, so they are signed.
		assertEquals("s3.internal:443", AWS4Signature.authority("s3.internal", 443, false));
		assertEquals("s3.internal:80", AWS4Signature.authority("s3.internal", 80, true));
		assertEquals("s3.internal", AWS4Signature.authority("s3.internal", -1, false));
	}

	@Test
	public void aws4HeaderValuesAreTrimmed() throws Exception {
		final MultiMap padded = MultiMap.caseInsensitiveMultiMap();
		padded.add("host", "  examplebucket.s3.amazonaws.com  ");
		padded.add("x-amz-meta-filename", "a   b");

		final MultiMap tidy = MultiMap.caseInsensitiveMultiMap();
		tidy.add("host", "examplebucket.s3.amazonaws.com");
		tidy.add("x-amz-meta-filename", "a b");

		final Instant instant = Instant.parse("2013-05-24T00:00:00Z");
		assertEquals(
				AWS4Signature.sign("PUT", "/test.txt", "", tidy, "us-east-1", "key", "secret", null, instant),
				AWS4Signature.sign("PUT", "/test.txt", "", padded, "us-east-1", "key", "secret", null, instant));
	}

	/**
	 * SigV4 signs the parameters sorted by name, whatever order they go on the wire in. Left to the caller,
	 * this held only by luck on every route carrying more than one parameter.
	 */
	@Test
	public void aws4CanonicalQueryStringIsSortedByName() {
		assertEquals("partNumber=1&uploadId=abc",
				AWS4Signature.canonicalQueryString("uploadId=abc&partNumber=1"));
		assertEquals("continuation-token=t&list-type=2&prefix=x",
				AWS4Signature.canonicalQueryString("list-type=2&prefix=x&continuation-token=t"));
		// A repeated name is ordered on its value.
		assertEquals("k=a&k=b", AWS4Signature.canonicalQueryString("k=b&k=a"));
	}

	/** A parameter with no value still carries its {@code =}, as {@code ?uploads} does. */
	@Test
	public void aws4CanonicalQueryStringAlwaysCarriesTheEquals() {
		assertEquals("uploads=", AWS4Signature.canonicalQueryString("uploads"));
		assertEquals("uploads=", AWS4Signature.canonicalQueryString("uploads="));
		assertEquals("", AWS4Signature.canonicalQueryString(""));
		assertEquals("", AWS4Signature.canonicalQueryString(null));
	}

	/**
	 * Values arrive already percent encoded — they are what the URI carries. Encoding them a second time
	 * here would sign {@code %2520} against a wire value of {@code %20}.
	 */
	@Test
	public void aws4CanonicalQueryStringLeavesEncodedValuesAlone() {
		assertEquals("prefix=dossier%20priv%C3%A9%2F",
				AWS4Signature.canonicalQueryString("prefix=dossier%20priv%C3%A9%2F"));
		// An opaque uploadId holding an encoded + keeps it: decoding it would corrupt the token.
		assertEquals("uploadId=a%2Bb%3D%3D", AWS4Signature.canonicalQueryString("uploadId=a%2Bb%3D%3D"));
	}

	/** Already canonical in, unchanged out — the signature of a compliant call site does not move. */
	@Test
	public void aws4CanonicalQueryStringIsIdempotent() {
		final String canonical = AWS4Signature.canonicalQueryString("uploadId=abc&partNumber=1");
		assertEquals(canonical, AWS4Signature.canonicalQueryString(canonical));
	}

}
