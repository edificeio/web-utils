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

import fr.wseduc.webutils.security.Blowfish;
import fr.wseduc.webutils.security.Md5;
import fr.wseduc.webutils.security.Sha256;
import org.junit.Test;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;

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

}
