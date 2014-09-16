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


import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Base64;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JWT {

	private static final Logger log = LoggerFactory.getLogger(JWT.class);
	private final HttpClient httpClient;
	private final String certsPath;
	private final ConcurrentMap<String, PublicKey> certificates;

	private enum Algorithm {
		RS256("SHA256withRSA"), RS384("SHA384withRSA"), RS512("SHA512withRSA");

		private final String algo;

		private Algorithm(String s) {
			algo = s;
		}

		public String getAlgo(){
			return algo;
		}

	}

	public JWT(Vertx vertx, URI certsUri) {
		this.httpClient = vertx.createHttpClient()
				.setHost(certsUri.getHost())
				.setPort(certsUri.getPort())
				.setSSL("https".equals(certsUri.getScheme()))
				.setMaxPoolSize(4)
				.setKeepAlive(false);
		this.certsPath = certsUri.getPath();
		this.certificates = new ConcurrentHashMap<>();
		findCertificates(null);
	}

	private void findCertificates(final VoidHandler handler) {
		httpClient.getNow(certsPath, new Handler<HttpClientResponse>() {
			@Override
			public void handle(HttpClientResponse response) {
				if (response.statusCode() == 200) {
					response.bodyHandler(new Handler<Buffer>() {
						@Override
						public void handle(Buffer buffer) {
							JsonObject c =  new JsonObject(buffer.toString("UTF-8"));
							try {
								CertificateFactory f = CertificateFactory.getInstance("X.509");
								for (String a : c.getFieldNames()) {
									String cert = c.getString(a);
									if (cert != null) {
										try {
											Certificate certificate = f.generateCertificate(
													new ByteArrayInputStream(cert.getBytes("UTF-8")));
											PublicKey p = certificate.getPublicKey();
											certificates.putIfAbsent(a, p);
										} catch (CertificateException | UnsupportedEncodingException e) {
											log.error(e.getMessage(), e);
										}
									}
								}
							} catch (CertificateException e) {
								log.error(e.getMessage(), e);
							} finally {
								if (handler != null) {
									handler.handle(null);
								}
							}
						}
					});
				} else if (handler != null) {
					handler.handle(null);
				}
			}
		});
	}

	private static String base64Decode(String s) throws UnsupportedEncodingException {
		return new String(base64DecodeToByte(s), "UTF-8");
	}

	private static byte[] base64DecodeToByte(String s) {
		int repeat = 4 - (s.length() % 4);
		StringBuilder b = new StringBuilder("");
		for (int i = 0; i < repeat; i++) {
			b.append("=");
		}
		return Base64.decode(s + b.toString(), Base64.URL_SAFE);
	}

	public void verifyAndGet(final String token, final Handler<JsonObject> handler) {
		String[] t = token.split("\\.");
		if (t.length != 3) {
			handler.handle(null);
			return;
		}
		JsonObject header;
		try {
			header = new JsonObject(base64Decode(t[0]));
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage(), e);
			handler.handle(null);
			return;
		}
		final String kid = header.getString("kid");
		if (kid != null) {
			PublicKey publicKey = certificates.get(kid);
			if (publicKey == null) {
				findCertificates(new VoidHandler() {
					@Override
					protected void handle() {
						handler.handle(verifyAndGet(token, certificates.get(kid)));
					}
				});
			} else {
				handler.handle(verifyAndGet(token, publicKey));
			}
		}
	}

	public static JsonObject verifyAndGet(String token, PublicKey publicKey) {
		String[] t = token.split("\\.");
		if (t.length != 3 || publicKey == null) {
			return null;
		}
		try {
			JsonObject header = new JsonObject(base64Decode(t[0]));
			JsonObject payload = new JsonObject(base64Decode(t[1]));
			byte[] signature = base64DecodeToByte(t[2]);
			Signature sign = Signature.getInstance(Algorithm.valueOf(header.getString("alg")).getAlgo());
			sign.initVerify(publicKey);
			sign.update((t[0] + "." + t[1]).getBytes("UTF-8"));
			if (sign.verify(signature)) {
				return payload;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

}
