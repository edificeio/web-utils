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

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import io.vertx.core.Vertx;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RSA
{
  private static PKCS8EncodedKeySpec getKey(Vertx vertx, String path)
  {
    String key = vertx.fileSystem().readFileBlocking(path).toString();

    key = key
      .replaceAll("-+\\s*BEGIN\\s+PRIVATE\\s+KEY\\s*-+", "")
      .replaceAll("-+\\s*END\\s+PRIVATE\\s+KEY\\s*-+", "")
      .replaceAll("\\s+", "")
      .replaceAll("\n", "");

    return new PKCS8EncodedKeySpec(Base64.getDecoder().decode(key));
  }

  public static PrivateKey loadPrivateKey(Vertx vertx, String privateKeyPath) throws NoSuchAlgorithmException, InvalidKeySpecException
  {
    if (privateKeyPath != null && !privateKeyPath.trim().isEmpty() && vertx.fileSystem().existsBlocking(privateKeyPath))
    {
			PKCS8EncodedKeySpec privateKeySpec = getKey(vertx, privateKeyPath);
			return KeyFactory.getInstance("RSA").generatePrivate(privateKeySpec);
    }
    else
      return null;
  }

  public static PublicKey loadPublicKey(Vertx vertx, String privateKeyPath) throws NoSuchAlgorithmException, InvalidKeySpecException
  {
    RSAPrivateCrtKey pkey = (RSAPrivateCrtKey) loadPrivateKey(vertx, privateKeyPath);

    if(pkey != null)
    {
      RSAPublicKeySpec kSpec = new RSAPublicKeySpec(pkey.getModulus(), pkey.getPublicExponent());
      return KeyFactory.getInstance("RSA").generatePublic(kSpec);
    }
    else
      return null;
  }

  public static String sign(String plainText, PrivateKey privateKey)
    throws NoSuchAlgorithmException, InvalidKeyException, SignatureException
  {
    Signature privateSignature = Signature.getInstance("SHA256withRSA");
    privateSignature.initSign(privateKey);
    privateSignature.update(plainText.getBytes(UTF_8));

    byte[] signature = privateSignature.sign();

    return Base64.getEncoder().encodeToString(signature);
  }

	public static String signFile(String path, PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException
	{
    File file = new File(path);
    List<InputStream> fileStreams = new LinkedList<InputStream>();

		if (!file.isDirectory())
			fileStreams.add(new FileInputStream(file));
		else
      collectFiles(file, fileStreams);

    Signature privateSignature = Signature.getInstance("SHA256withRSA");
    privateSignature.initSign(privateKey);

    byte[] buf = new byte[2048];
    int len;

    for(InputStream in : fileStreams)
    {
      while ((len = in.read(buf)) != -1)
        privateSignature.update(buf, 0, len);

      in.close();
    }

		return Base64.getEncoder().encodeToString(privateSignature.sign());
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

  public static boolean verifyFile(String path, String signature, PublicKey publicKey)
    throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException
	{
    File file = new File(path);
    List<InputStream> fileStreams = new LinkedList<InputStream>();

		if (!file.isDirectory())
			fileStreams.add(new FileInputStream(file));
		else
      collectFiles(file, fileStreams);

    Signature publicSignature = Signature.getInstance("SHA256withRSA");
    publicSignature.initVerify(publicKey);

    byte[] buf = new byte[2048];
    int len;

    for(InputStream in : fileStreams)
    {
      while ((len = in.read(buf)) != -1)
        publicSignature.update(buf, 0, len);

      in.close();
    }

		return publicSignature.verify(Base64.getDecoder().decode(signature));
	}

  public static boolean verify(String plainText, String signature, PublicKey publicKey)
    throws NoSuchAlgorithmException, InvalidKeyException, SignatureException
  {
    Signature publicSignature = Signature.getInstance("SHA256withRSA");
    publicSignature.initVerify(publicKey);
    publicSignature.update(plainText.getBytes(UTF_8));

    byte[] signatureBytes = Base64.getDecoder().decode(signature);

    return publicSignature.verify(signatureBytes);
  }
}