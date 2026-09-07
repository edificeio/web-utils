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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Percent encoding as RFC 3986 defines it, which is what AWS SigV4 canonicalisation requires and what
 * {@link java.net.URLEncoder} does not provide: URLEncoder emits {@code application/x-www-form-urlencoded},
 * where a space becomes {@code +}, {@code ~} becomes {@code %7E} and {@code *} is left untouched. Each of
 * those diverges from the form the server recomputes — a wrong key for the first, SignatureDoesNotMatch for
 * the other two.
 */
public class UriEncoder {

	/** Uppercase, as AWS requires of the hexadecimal value of an encoded byte. */
	private static final char[] HEX = "0123456789ABCDEF".toCharArray();

	/**
	 * Encodes a value meant to sit inside a single URI component — a query string value, or one path segment
	 * taken alone. Everything outside the unreserved set is percent encoded, {@code /} included.
	 */
	public static String encode(String value) {
		return encode(value, false);
	}

	/**
	 * Encodes a whole path: same rules as {@link #encode(String)}, except {@code /} is left as the separator
	 * it is. Empty segments, leading and trailing separators included, are preserved as they are.
	 */
	public static String encodePath(String path) {
		return encode(path, true);
	}

	/**
	 * The exact inverse of {@link #encode(String)} and {@link #encodePath(String)}. Unlike
	 * {@link java.net.URLDecoder}, a {@code +} decodes to a literal plus rather than to a space, and an
	 * incomplete or malformed escape is left untouched instead of raising.
	 */
	public static String decode(String value) {
		if (value == null) {
			return null;
		}
		final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		final ByteArrayOutputStream decoded = new ByteArrayOutputStream(bytes.length);
		for (int i = 0; i < bytes.length; i++) {
			if (bytes[i] == '%' && i + 2 < bytes.length) {
				final int high = Character.digit(bytes[i + 1], 16);
				final int low = Character.digit(bytes[i + 2], 16);
				if (high >= 0 && low >= 0) {
					decoded.write((high << 4) + low);
					i += 2;
					continue;
				}
			}
			decoded.write(bytes[i]);
		}
		return new String(decoded.toByteArray(), StandardCharsets.UTF_8);
	}

	private static String encode(String value, boolean keepSeparator) {
		if (value == null) {
			return null;
		}
		final StringBuilder encoded = new StringBuilder(value.length());
		for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
			if (isUnreserved(b) || (keepSeparator && b == '/')) {
				encoded.append((char) b);
			} else {
				encoded.append('%').append(HEX[(b >> 4) & 0xF]).append(HEX[b & 0xF]);
			}
		}
		return encoded.toString();
	}

	/** The unreserved set of RFC 3986 §2.3. A byte of a multi byte UTF-8 sequence is negative, so encoded. */
	private static boolean isUnreserved(byte b) {
		return (b >= 'A' && b <= 'Z') || (b >= 'a' && b <= 'z') || (b >= '0' && b <= '9')
				|| b == '-' || b == '.' || b == '_' || b == '~';
	}

}
