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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class Md5 {

	public static String hash(String input) throws NoSuchAlgorithmException {
		if(input == null) return null;
		MessageDigest digest = MessageDigest.getInstance("MD5");
		digest.update(input.getBytes(), 0, input.length());
		String hash = new BigInteger(1, digest.digest()).toString(16);
		while (hash.length() < 32) {
			hash = "0" + hash;
		}
		return hash;
	}

	public static byte[] hash(byte [] input) throws NoSuchAlgorithmException {
		if(input == null) return null;
		MessageDigest digest = MessageDigest.getInstance("MD5");
		digest.update(input, 0, input.length);
		return digest.digest();
	}

	public static String hashFile(String path) throws IOException, NoSuchAlgorithmException
	{
			File file = new File(path);
			List<InputStream> fileStreams = new LinkedList<InputStream>();

			MessageDigest digest = MessageDigest.getInstance("MD5");

			if (!file.isDirectory())
				fileStreams.add(new FileInputStream(file));
			else
				collectFiles(file, fileStreams);

			DigestInputStream dis = new DigestInputStream(new SequenceInputStream(Collections.enumeration(fileStreams)), digest);

			while(dis.read() != -1);
			dis.close();
			String hash = new BigInteger(1, digest.digest()).toString(16);
			while (hash.length() < 32) {
				hash = "0" + hash;
			}
			return hash;
	}

	private static void collectFiles(File directory, List<InputStream> fileInputStreams) throws IOException
	{
		File[] files = directory.listFiles();

		fileInputStreams.add(new ByteArrayInputStream(directory.getName().getBytes(StandardCharsets.UTF_8)));

		if (files != null)
		{
			Arrays.sort(files, Comparator.comparing(File::getName));

			for (File file : files)
			{
				if (file.isDirectory())
						collectFiles(file, fileInputStreams);
				else
						fileInputStreams.add(new FileInputStream(file));
			}
		}
	}

	public static boolean equality(byte [] input, byte [] sum) throws NoSuchAlgorithmException {
		return Arrays.equals(hash(input), sum);
	}

}
