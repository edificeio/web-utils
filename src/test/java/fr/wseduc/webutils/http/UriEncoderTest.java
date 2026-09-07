/*
 * Copyright © Open Digital Education, 2026
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

package fr.wseduc.webutils.http;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UriEncoderTest {

	@Test
	public void unreservedCharactersAreLeftAlone() {
		assertEquals("azAZ09-._~", UriEncoder.encode("azAZ09-._~"));
	}

	/**
	 * The three divergences of {@link java.net.URLEncoder} that motivate this encoder: a space encoded as
	 * {@code +} stores a wrong key, and {@code ~} / {@code *} encoded the wrong way have the server recompute
	 * a different canonical request — SignatureDoesNotMatch.
	 */
	@Test
	public void encodesWhatUrlEncoderGetsWrong() {
		assertEquals("a%20b.pdf", UriEncoder.encode("a b.pdf"));
		assertEquals("~", UriEncoder.encode("~"));
		assertEquals("%2A", UriEncoder.encode("*"));
		assertEquals("%2B", UriEncoder.encode("+"));
	}

	/** AWS requires the hexadecimal value of an encoded byte to be uppercase. */
	@Test
	public void escapesUseUppercaseHex() {
		assertEquals("%3A%2F%3F%23%5B%5D%40", UriEncoder.encode(":/?#[]@"));
	}

	@Test
	public void nonAsciiIsEncodedAsUtf8() {
		assertEquals("%C3%A9l%C3%A8ve.pdf", UriEncoder.encode("élève.pdf"));
	}

	@Test
	public void separatorIsEncodedInAValueAndKeptInAPath() {
		assertEquals("a%2Fb", UriEncoder.encode("a/b"));
		assertEquals("a/b%20c/d~e", UriEncoder.encodePath("a/b c/d~e"));
	}

	/** A segment split on {@code /} would silently drop the trailing one, and with it a directory marker. */
	@Test
	public void encodePathPreservesEmptySegments() {
		assertEquals("/a/b/", UriEncoder.encodePath("/a/b/"));
	}

	@Test
	public void decodeIsTheInverseOfEncode() {
		final String key = "dossier privé/rapport final (v2)*~.pdf";
		assertEquals(key, UriEncoder.decode(UriEncoder.encodePath(key)));
	}

	/** Where {@link java.net.URLDecoder} would hand back a space, and corrupt an opaque identifier. */
	@Test
	public void decodeKeepsPlusLiteral() {
		assertEquals("a+b", UriEncoder.decode("a+b"));
	}

	@Test
	public void decodeLeavesAMalformedEscapeUntouched() {
		assertEquals("100% sûr", UriEncoder.decode("100%%20s%C3%BBr"));
		assertEquals("%2", UriEncoder.decode("%2"));
	}

}
