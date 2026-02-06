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

package fr.wseduc.webutils.request;

import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.time.Instant;

import static fr.wseduc.webutils.request.RequestUtils.getTokenHeader;
import static fr.wseduc.webutils.request.RequestUtils.getUserAgent;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class AccessLoggerJson extends AccessLogger {

  protected static final java.util.logging.Logger log = java.util.logging.Logger.getLogger("ACCESS");

	/**
	 * Log the following line if the user is <strong>not</strong> authenticated :
	 * <pre>“ip” “verb uri” “user-agent”</pre>
	 *
	 * Log the following line if the user is authenticated :
	 * <pre>“ip” “verb uri” “user-agent” - userId sessionId tokenId</pre>
	 *
	 * @param request Incoming user request
	 * @param handler Downstream process ({@code null} will always be supplied
	 */
	public void log(HttpServerRequest request, Handler<Void> handler) {
		log.finest(formatLog(request, null));
		handler.handle(null);
	}

	protected String formatLog(final HttpServerRequest request, final String userId) {
    JsonObject logEntry = new JsonObject()
      .put("timestamp", Instant.now().toString())
      .put("ip", Renders.getIp(request))
      .put("method", request.method().toString())
      .put("path", request.path())
      .put("query", request.query())
      .put("userAgent", getUserAgent(request));
    if(userId != null) {
      logEntry.put("userId", userId);
    }

    if(request instanceof SecureHttpServerRequest) {
      JsonObject auth = getAuthenticatedUserInfo((SecureHttpServerRequest) request);
      logEntry.mergeIn(auth);
    }

    return logEntry.encode();
	}

	private JsonObject getAuthenticatedUserInfo(final SecureHttpServerRequest request) {
    final JsonObject session = request.getSession();
    final String userId = (session == null || isBlank(session.getString("externalId")))
      ? UNAUTHENTICATED_USER_ID
      : session.getString("externalId");
    final String tokenId = getTokenHeader(request).orElse(NO_TOKEN_ID);
    final String sessionId = CookieHelper.getInstance().getSigned("oneSessionId", request);
    final String cookieId = isBlank(sessionId) ? NO_SESSION_COOKIE : sessionId;

    return new JsonObject()
      .put("userId", userId)
      .put("sessionId", cookieId)
      .put("tokenId", tokenId);
	}

	private String getQuery(HttpServerRequest request) {
		if (request.query() != null) {
			return "?" + request.query();
		}
		return "";
	}

}
