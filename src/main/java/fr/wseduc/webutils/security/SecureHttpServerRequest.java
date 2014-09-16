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

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.net.NetSocket;

public class SecureHttpServerRequest implements HttpServerRequest {

	private final HttpServerRequest request;
	private JsonObject session;
	private final Map<String, String> attributes;
	private Buffer body;
	private boolean end;

	public SecureHttpServerRequest(HttpServerRequest request) {
		this.request = request;
		this.attributes = new HashMap<>();
	}

	@Override
	public HttpServerRequest dataHandler(Handler<Buffer> handler) {
		request.dataHandler(handler);
		return this;
	}

	@Override
	public HttpServerRequest pause() {
		request.pause();
		return this;
	}

	@Override
	public HttpServerRequest resume() {
		request.resume();
		return this;
	}

	@Override
	public HttpServerRequest endHandler(final Handler<Void> endHandler) {
		if (end) {
			if (endHandler != null) {
				endHandler.handle(null);
			}
			return this;
		}
		request.endHandler(new Handler<Void>() {
			@Override
			public void handle(Void event) {
				end = true;
				if (endHandler != null) {
					endHandler.handle(null);
				}
			}
		});
		return this;
	}

	@Override
	public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
		request.exceptionHandler(handler);
		return this;
	}

	@Override
	public HttpVersion version() {
		return request.version();
	}

	@Override
	public String method() {
		return request.method();
	}

	@Override
	public String uri() {
		return request.uri();
	}

	@Override
	public String path() {
		return request.path();
	}

	@Override
	public String query() {
		return request.query();
	}

	@Override
	public HttpServerResponse response() {
		return request.response();
	}

	@Override
	public MultiMap headers() {
		return request.headers();
	}

	@Override
	public MultiMap params() {
		return request.params();
	}

	@Override
	public InetSocketAddress remoteAddress() {
		return request.remoteAddress();
	}

	@Override
	public InetSocketAddress localAddress() {
		return request.localAddress();
	}

	@Override
	public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
		return request.peerCertificateChain();
	}

	@Override
	public URI absoluteURI() {
		return request.absoluteURI();
	}

	@Override
	public HttpServerRequest bodyHandler(final Handler<Buffer> bodyHandler) {
		if (body != null) {
			if (bodyHandler != null) {
				bodyHandler.handle(body);
			}
			return this;
		}
		request.bodyHandler(new Handler<Buffer>() {
			@Override
			public void handle(Buffer event) {
				body = event;
				if (bodyHandler != null) {
					bodyHandler.handle(body);
				}
			}
		});
		return this;
	}

	@Override
	public NetSocket netSocket() {
		return request.netSocket();
	}

	@Override
	public HttpServerRequest expectMultiPart(boolean expect) {
		request.expectMultiPart(expect);
		return this;
	}

	@Override
	public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> uploadHandler) {
		request.uploadHandler(uploadHandler);
		return this;
	}

	@Override
	public MultiMap formAttributes() {
		return request.formAttributes();
	}

	public JsonObject getSession() {
		return session;
	}

	public void setSession(JsonObject session) {
		this.session = session;
	}

	public void setAttribute(String attr, String value) {
		attributes.put(attr, value);
	}

	public String getAttribute(String attr) {
		return attributes.get(attr);
	}

}
