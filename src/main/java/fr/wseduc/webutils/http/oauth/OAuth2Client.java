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

package fr.wseduc.webutils.http.oauth;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.wseduc.webutils.security.JWT;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.http.impl.HttpClientResponseImpl;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.DecodeException;

import fr.wseduc.webutils.http.Renders;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetSocket;
import org.apache.commons.lang3.StringUtils;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class OAuth2Client {

	protected static final Logger log = LoggerFactory.getLogger(OAuth2Client.class);
	private final URI uri;
	private final String clientId;
	private final String secret;
	private final String authorizeUrn;
	private final String tokenUrn;
	private final String redirectUri;
	private final HttpClient httpClient;

	public OAuth2Client(URI uri, String clientId, String secret, String authorizeUrn,
			String tokenUrn, String redirectUri, Vertx vertx, int poolSize, boolean keepAlive) {
		HttpClientOptions options = new HttpClientOptions()
				.setDefaultHost(uri.getHost())
				.setDefaultPort(uri.getPort())
				.setSsl("https".equals(uri.getScheme()))
				.setMaxPoolSize(poolSize)
				.setKeepAlive(keepAlive);
		this.httpClient = vertx.createHttpClient(options);
		this.uri = uri;
		this.clientId = clientId;
		this.secret = secret;
		this.authorizeUrn = authorizeUrn;
		this.tokenUrn = tokenUrn;
		this.redirectUri = redirectUri;
	}

	public OAuth2Client(URI uri, String clientId, String secret, String authorizeUrn,
						String tokenUrn, String redirectUri, Vertx vertx, int poolSize) {
		this(uri, clientId, secret, authorizeUrn, tokenUrn, redirectUri, vertx, poolSize, false);
	}

	public OAuth2Client(URI uri, String clientId, String secret, String redirectUri, Vertx vertx) {
		this(uri, clientId, secret, "/oauth2/auth", "/oauth2/token", redirectUri, vertx, 16);
	}

	public void authorizeRedirect(HttpServerRequest request, String state, String scope) {
		authorizeRedirect(request, state, null, scope);
	}

	public void authorizeRedirect(HttpServerRequest request, String state, String nonce, String scope) {
		String rUri = "";
		try {
			rUri = URLEncoder.encode(redirectUri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String params = "response_type=code&client_id=" + clientId + "&state=" + state +
				(isNotEmpty(nonce) ? "&nonce=" + nonce : "") +
				"&redirect_uri=" + rUri + "&scope=" + scope;

		Renders.redirect(request, uri.toString(), authorizeUrn + "?" + params);
	}

	public void authorizationCodeToken(HttpServerRequest request, String state,
			Handler<JsonObject> handler) {
		authorizationCodeToken(request, state, true, handler);
	}

	public void authorizationCodeToken(HttpServerRequest request, String state, boolean basic,
			Handler<JsonObject> handler) {
		String s = request.params().get("state");
		String code = request.params().get("code");
		String error = request.params().get("error");
		JsonObject r = new JsonObject();
		if (state != null && !state.equals(s)) {
			handler.handle(r.put("error", "invalid_state"));
			return;
		}
		if (error != null) {
			handler.handle(r.put("error", error));
			return;
		}
		try {
			getAccessToken(code, basic, handler);
		} catch (UnsupportedEncodingException e) {
			handler.handle(r.put("error", e.getMessage()));
		}
	}

	public void getAccessToken(String code,
			final Handler<JsonObject> handler) throws UnsupportedEncodingException {
		getAccessToken(code, true, handler);
	}

	public void getAccessToken(String code, boolean basic,
			final Handler<JsonObject> handler) throws UnsupportedEncodingException {
		HeadersMultiMap headers = new HeadersMultiMap()
				.add("Content-Type", "application/x-www-form-urlencoded")
				.add("Accept", "application/json; charset=UTF-8");
		StringBuilder body = new StringBuilder("grant_type=authorization_code&code=" + code + "&redirect_uri=" + redirectUri);
		if (basic) {
			headers.add("Authorization", "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + secret).getBytes()));
		} else {
			body.append("&client_id=" + clientId + "&client_secret=" + secret);
		}
    String stringLog = "POST %s %d rt=%d";
    long startTime = System.currentTimeMillis();
		httpClient.request(
				new RequestOptions()
						.setMethod(HttpMethod.POST)
						.setURI(this.uri.getPath() + tokenUrn)
						.setHeaders(headers))
				.flatMap(request -> request.send(body.toString()))
				.onSuccess(response -> {
          long endTime = System.currentTimeMillis();
          long responseTime = endTime - startTime;
          log.info(String.format(stringLog, response.request().path(), response.statusCode(), responseTime));
          response.bodyHandler(buffer -> {
            try {
              JsonObject j = new JsonObject(buffer.toString("UTF-8"));
              if (response.statusCode() == 200) {
                JsonObject json = new JsonObject()
                  .put("status", "ok")
                  .put("token", j);
                handler.handle(json);
              } else {
                handler.handle(j.put("statusCode", response.statusCode()));
              }
            }
            catch(DecodeException e)
            {
              handler.handle(new JsonObject().put("statusCode", response.statusCode()));
            }
          });
        })
				.onFailure(except -> log.error("Error getting access token.", except));
	}

	public void clientCredentialsToken(String scope,
			final Handler<JsonObject> handler) throws UnsupportedEncodingException {
		StringBuilder body = new StringBuilder("grant_type=client_credentials");
		if (scope != null && !scope.trim().isEmpty()) {
			body.append("&scope=" + scope);
		}

		httpClient.request(
				new RequestOptions()
						.setMethod(HttpMethod.POST)
						.setURI(this.uri.getPath() + tokenUrn)
						.setHeaders(new HeadersMultiMap()
								.add("Authorization", "Basic " + Base64.getEncoder().encode((clientId + ":" + secret).getBytes("UTF-8")))
								.add("Content-Type", "application/x-www-form-urlencoded")
								.add("Accept", "application/json; charset=UTF-8")))
				.flatMap(request -> request.send(body.toString()))
				.onSuccess(response -> response.bodyHandler(r -> {
                    JsonObject j = new JsonObject(r.toString("UTF-8"));
                    if (response.statusCode() == 200) {
                        JsonObject json = new JsonObject()
                                .put("status", "ok")
                                .put("token", j);
                        handler.handle(json);
                    } else {
                        handler.handle(j.put("statusCode", response.statusCode()));
                    }
                }))
				.onFailure(except -> log.error("Error getting client credentials token.", except));
	}

	public void client2LO(JsonObject payload, PrivateKey privateKey, final Handler<JsonObject> handler) throws Exception{
		String jwt = JWT.encodeAndSign(payload, null, privateKey);
		httpClient.request(new RequestOptions()
				.setMethod(HttpMethod.POST)
				.setURI(this.uri.getPath() + tokenUrn)
				.setHeaders(new HeadersMultiMap()
						.add("Content-Type", "application/x-www-form-urlencoded")
						.add("Accept", "application/json; charset=UTF-8")))
				.flatMap(request -> request.send(new StringBuilder("grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer").append("&assertion=").append(jwt).toString()))
				.onSuccess(response -> response.bodyHandler(r -> {
                    JsonObject j = new JsonObject(r.toString("UTF-8"));
                    if (response.statusCode() == 200) {

                        JsonObject json = new JsonObject()
                                .put("status", "ok")
                                .put("token", j);
                        handler.handle(json);
                    } else {
                        handler.handle(j.put("statusCode", response.statusCode()));
                    }
                }))
				.onFailure(except -> log.error("Error getting 2LO access token.", except));
	}


	public void getProtectedResource(String path, String accessToken,
			Handler<HttpClientResponse> handler) {
		getProtectedResource(path, accessToken, "application/json; charset=UTF-8", handler);
	}

	public void getProtectedResource(String path, String accessToken, Map<String, String> headers,
			Handler<HttpClientResponse> handler) {
		createRequest(HttpMethod.GET, this.uri.getPath() + path, accessToken, headers)
		.flatMap(HttpClientRequest::send)
		.onSuccess(handler)
		.onFailure(th -> onSendRequestFail(th, path, handler));
	}

	public void getProtectedResource(String path, String accessToken, String acceptMimeType,
			Handler<HttpClientResponse> handler) {
		createRequest(HttpMethod.GET, this.uri.getPath() + path, accessToken, extraHeadersForMimeType(acceptMimeType))
				.flatMap(HttpClientRequest::send)
				.onSuccess(handler)
				.onFailure(th -> onSendRequestFail(th, path, handler));
	}

	public void postProtectedResource(String path, String accessToken, String body,
									 Handler<HttpClientResponse> handler) {
		postProtectedResource(path, accessToken, "application/json; charset=UTF-8", body, handler);
	}

	public void postProtectedResource(String path, String accessToken, Map<String, String> headers,
									  String body, Handler<HttpClientResponse> handler) {
		createRequest(HttpMethod.POST, this.uri.getPath() + path, accessToken, headers)
				.flatMap(r -> r.send(body))
				.onSuccess(handler)
				.onFailure(th -> onSendRequestFail(th, path, handler));
	}

	public void postProtectedResource(String path, String accessToken, String acceptMimeType,
			String body, Handler<HttpClientResponse> handler) {
		createRequest(HttpMethod.POST, this.uri.getPath() + path, accessToken, extraHeadersForMimeType(acceptMimeType))
				.flatMap(r -> r.send(body))
				.onSuccess(handler)
				.onFailure(th -> onSendRequestFail(th, path, handler));
	}

	public void putProtectedResource(String path, String accessToken, String body,
									  Handler<HttpClientResponse> handler) {
		putProtectedResource(path, accessToken, "application/json; charset=UTF-8", body, handler);
	}

	public void putProtectedResource(String path, String accessToken, Map<String, String> headers,
									 String body, Handler<HttpClientResponse> handler) {
		createRequest(HttpMethod.PUT, this.uri.getPath() + path, accessToken, headers)
				.flatMap(r -> r.send(body))
				.onSuccess(handler)
				.onFailure(th -> onSendRequestFail(th, path, handler));
	}

	public void putProtectedResource(String path, String accessToken, String acceptMimeType,
			String body, Handler<HttpClientResponse> handler) {
		createRequest(HttpMethod.PUT, this.uri.getPath() + path, accessToken, extraHeadersForMimeType(acceptMimeType))
				.flatMap(r -> r.send(body))
				.onSuccess(handler)
				.onFailure(th -> onSendRequestFail(th, path, handler));

	}

	public void deleteProtectedResource(String path, String accessToken,
									  Handler<HttpClientResponse> handler) {
		deleteProtectedResource(path, accessToken, "application/json; charset=UTF-8", handler);
	}

	public void deleteProtectedResource(String path, String accessToken, Map<String, String> headers,
										Handler<HttpClientResponse> handler) {
		createRequest(HttpMethod.DELETE, this.uri.getPath() + path, accessToken, headers)
				.flatMap(HttpClientRequest::send)
				.onSuccess(handler)
				.onFailure(th -> onSendRequestFail(th, path, handler));
	}

	public void deleteProtectedResource(String path, String accessToken, String acceptMimeType,
									  Handler<HttpClientResponse> handler) {
		createRequest(HttpMethod.DELETE, this.uri.getPath() + path, accessToken, extraHeadersForMimeType(acceptMimeType))
				.flatMap(HttpClientRequest::send)
				.onSuccess(handler)
				.onFailure(th -> onSendRequestFail(th, path, handler));
	}

	private void onSendRequestFail(Throwable th, String path, Handler<HttpClientResponse> handler) {
		log.error("Error while calling " + path, th);
		handler.handle(createFake500Response(th, path));
	}

	private HttpClientResponse createFake500Response(Throwable th, String path) {
		// TODO vertx4 improve
		return new HttpClientResponse() {
			@Override
			public HttpClientResponse fetch(long amount) {
				return this;
			}

			@Override
			public HttpClientResponse resume() {
				return this;
			}

			@Override
			public HttpClientResponse exceptionHandler(Handler<Throwable> handler) {
				return this;
			}

			@Override
			public HttpClientResponse handler(Handler<Buffer> handler) {
				return this;
			}

			@Override
			public HttpClientResponse pause() {
				return this;
			}

			@Override
			public HttpClientResponse endHandler(Handler<Void> endHandler) {
				return this;
			}

			@Override
			public NetSocket netSocket() {
				return null;
			}

			@Override
			public HttpVersion version() {
				return HttpVersion.HTTP_1_1;
			}

			@Override
			public int statusCode() {
				return 500;
			}

			@Override
			public String statusMessage() {
				return "server.error";
			}

			@Override
			public MultiMap headers() {
				return new HeadersMultiMap();
			}

			@Override
			public @Nullable String getHeader(String headerName) {
				return null;
			}

			@Override
			public String getHeader(CharSequence headerName) {
				return null;
			}

			@Override
			public @Nullable String getTrailer(String trailerName) {
				return null;
			}

			@Override
			public MultiMap trailers() {
				return null;
			}

			@Override
			public List<String> cookies() {
				return null;
			}

			@Override
			public Future<Buffer> body() {
				return null;
			}

			@Override
			public Future<Void> end() {
				return null;
			}

			@Override
			public HttpClientResponse customFrameHandler(Handler<HttpFrame> handler) {
				return this;
			}

			@Override
			public HttpClientRequest request() {
				return null;
			}

			@Override
			public HttpClientResponse streamPriorityHandler(Handler<StreamPriority> handler) {
				return this;
			}
		};
	}

	private Map<String, String> extraHeadersForMimeType(String acceptMimeType) {
		final Map<String, String> extraHeaders = new HashMap<>();
		if(StringUtils.isBlank(acceptMimeType)) {
			extraHeaders.put("Accept", acceptMimeType);
		}
		return extraHeaders;
	}

	private Future<HttpClientRequest> createRequest(final HttpMethod method, String uri, String accessToken, Map<String, String> extraHeaders) {
		final MultiMap headers = new HeadersMultiMap();
		if(extraHeaders != null) {
			extraHeaders.forEach(headers::add);
		}
		if(!headers.contains("Accept")) {
			headers.add("Accept", "application/json; charset=UTF-8");
		}
		if(StringUtils.isNotBlank(accessToken)) {
			headers.add("Authorization", "Bearer " + accessToken);
		}
		final RequestOptions options = new RequestOptions()
			.setMethod(method)
			.setHeaders(headers);
		if(uri.startsWith("http://") || uri.startsWith("https://")) {
			options.setAbsoluteURI(uri);
		} else {
			options.setURI(uri);
		}
		return httpClient.request(options);
	}


	public void close() {
		if (httpClient != null) {
			httpClient.close();
		}
	}

}
