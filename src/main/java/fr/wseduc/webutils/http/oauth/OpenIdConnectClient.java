/*
 * Copyright © WebServices pour l'Éducation, 2016
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

import fr.wseduc.webutils.security.JWT;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import java.net.URI;
import java.net.URISyntaxException;

public final class OpenIdConnectClient extends OAuth2Client {

	private final JWT jwt;

	public OpenIdConnectClient(URI uri, String clientId, String secret, String authorizeUrn,
			String tokenUrn, String redirectUri, Vertx vertx, int poolSize, String certificatesUri)
			throws URISyntaxException {
		super(uri, clientId, secret, authorizeUrn, tokenUrn, redirectUri, vertx, poolSize);
		this.jwt = new JWT(vertx, secret, (certificatesUri != null ? new URI(certificatesUri) : null));
	}

	@Override
	public void authorizationCodeToken(HttpServerRequest request, String state, Handler<JsonObject> handler) {
		authorizationCodeToken(request, state, true, handler);
	}

	@Override
	public void authorizationCodeToken(HttpServerRequest request, String state, boolean basic,
			final Handler<JsonObject> handler) {
		super.authorizationCodeToken(request, state, basic, new Handler<JsonObject>() {
			@Override
			public void handle(JsonObject res) {
				if ("ok".equals(res.getString("status"))) {
					String idToken = res.getObject("token", new JsonObject()).getString("id_token");
					jwt.verifyAndGet(idToken, handler);
				} else {
					handler.handle(null);
				}
			}
		});
	}

}
