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

package fr.wseduc.webutils.security;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

public class Sha256 {

	public static String hash(String input) throws NoSuchAlgorithmException {
		if(input == null) return null;
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.update(input.getBytes(), 0, input.length());
		String hash = new BigInteger(1, digest.digest()).toString(16);
		while (hash.length() < 64) {
			hash = "0" + hash;
		}
		return hash;
	}

	public static String hashBase64(String input) throws NoSuchAlgorithmException
	{
		if(input == null) return null;
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.update(input.getBytes(), 0, input.length());
		return Base64.getEncoder().encodeToString(digest.digest());
	}	

	public static byte[] hash(byte [] input) throws NoSuchAlgorithmException {
		if(input == null) return null;
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.update(input, 0, input.length);
		return digest.digest();
	}

	public static boolean equality(byte [] input, byte [] sum) throws NoSuchAlgorithmException {
		return Arrays.equals(hash(input), sum);
	}

}
