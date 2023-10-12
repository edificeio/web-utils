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

package fr.wseduc.webutils.http;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;

public class HttpClientUtils {

	public static void sendFile(Vertx vertx, String uri, int port, String content,
			MultiMap headers, String filename,
			String contentType, Handler<HttpClientResponse> handler) {
		HttpClientOptions options = new HttpClientOptions().setDefaultPort(port);
		Buffer buffer = Buffer.buffer();
		final String boundary = "dLV9Wyq26L_-JQxk6ferf-RT153LhOO";
		final String body = "--" + boundary + "\r\n" +
				"Content-Disposition: form-data; name=\"file\"; filename=\""+ filename +"\"\r\n" +
				"Content-Type: " + contentType + "\r\n" +
				"\r\n" +
				content + "\r\n" +
				"--" + boundary + "--\r\n";
		buffer.appendString(body);

		vertx.createHttpClient(options).request(new RequestOptions()
				.setMethod(HttpMethod.POST)
				.setURI(uri)
				.setHeaders(new HeadersMultiMap()
						.addAll(headers)
						.add("content-length", String.valueOf(buffer.length()))
						.add("content-type", "multipart/form-data; boundary=" + boundary)))
				.flatMap(request -> request.send(buffer))
				.onSuccess(handler);
	}

	public static void proxy(final HttpServerRequest req, HttpClient client) {
		proxy(req, client, null, null, null);
	}

	public static void proxy(final HttpServerRequest req, HttpClient client, String prefix) {
		proxy(req, client, prefix, null, null);
	}

	public static void proxy(final HttpServerRequest req, HttpClient client,
			String prefix, String replacement) {
		proxy(req, client, prefix, replacement, null);
	}

	public static void proxy(final HttpServerRequest req, HttpClient client,
			String prefix, String replacement, final JsonObject defaultResult) {
		String uri = req.uri();
		if (prefix != null && !prefix.trim().isEmpty()) {
			if (replacement != null && !replacement.trim().isEmpty()) {
				uri = uri.replaceFirst(prefix, replacement);
			} else {
				uri = uri.replaceFirst(req.path(), prefix + req.path());
			}
		}
		req.pause();
		client.request(new RequestOptions()
						.setMethod(req.method())
						.setURI(uri)
						.setHeaders(new HeadersMultiMap()
								.addAll(req.headers())
								.add("Host", req.host())))
				.map(request -> request.setChunked(true))
				.onSuccess(request -> {
					req.handler(data -> request.write(data));
					req.endHandler(v -> request.send().onSuccess(cRes -> {
						if (defaultResult != null && defaultResult.getString("content") != null &&
									(cRes.statusCode() < 200 || (cRes.statusCode() >= 300 &&
											cRes.statusCode() != 304))) {
								if (defaultResult.getJsonObject("headers") != null) {
									for (String header: defaultResult.getJsonObject("headers").fieldNames()) {
										req.response().headers().add(header,
												defaultResult.getJsonObject("headers").getString(header));
									}
								}
								if ("file".equals(defaultResult.getString("type"))) {
									req.response().sendFile(defaultResult.getString("content"));
								} else {
									req.response().end(defaultResult.getString("content"));
								}
							} else {
								req.response().setStatusCode(cRes.statusCode());
								req.response().headers().setAll(cRes.headers());
								req.response().setChunked(true);
								cRes.handler(data -> req.response().write(data));
								cRes.endHandler(event -> req.response().end());
							}
						}
					));
					req.resume();
				});
	}

}
