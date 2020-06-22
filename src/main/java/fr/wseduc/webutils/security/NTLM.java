/*
 * Copyright © Open Digital Education, 2020
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

package fr.wseduc.webutils.security;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class NTLM {

    public static String ntHash(String input) throws NoSuchAlgorithmException {
        if(input == null) return null;
        final byte[] unicodeInput = input.getBytes(StandardCharsets.UTF_16LE);
		String hash = new BigInteger(1, md4(unicodeInput)).toString(16);
		while (hash.length() < 32) {
			hash = "0" + hash;
		}
		return hash;
	}

    public static byte[] md4(byte [] input) throws NoSuchAlgorithmException {
        final MessageDigest md4 = MessageDigest.getInstance("MD4");
        return md4.digest(input);
    }

}
