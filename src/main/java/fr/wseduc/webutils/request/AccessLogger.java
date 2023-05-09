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
import static fr.wseduc.webutils.request.RequestUtils.getTokenHeader;
import static fr.wseduc.webutils.request.RequestUtils.getUserAgent;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class AccessLogger {

	protected static final io.vertx.core.logging.Logger log = LoggerFactory.getLogger(AccessLogger.class);
	public static final String UNAUTHENTICATED_USER_ID = "unauthenticated";
	public static final String NO_SESSION_COOKIE = "nocookie";
	public static final String NO_TOKEN_ID = "notoken";

	/**
	 * Log the following line if the user is <strong>not</strong> authenticated :
	 * <pre>“ip” “verb uri” “path” “user-agent”</pre>
	 *
	 * Log the following line if the user is authenticated :
	 * <pre>“ip” “verb uri” “path” “user-agent” - userId sessionId tokenId</pre>
	 *
	 * @param request Incoming user request
	 * @param handler Downstream process ({@code null} will always be supplied
	 */
	public void log(HttpServerRequest request, Handler<Void> handler) {
		log.trace(formatLog(request));
		handler.handle(null);
	}

	protected String formatLog(final HttpServerRequest request) {
		return String.format("\"%s\" \"%s\" \"%s%s\" \"%s\"%s",
				Renders.getIp(request), request.method(),
				request.path(), getQuery(request),
				getUserAgent(request), getAuthenticatedUserInfo(request));
	}

	private String getAuthenticatedUserInfo(final HttpServerRequest request) {
		if(request instanceof SecureHttpServerRequest) {
			final String userId;
			final String tokenId;
			final String cookieId;
			final JsonObject session = ((SecureHttpServerRequest) request).getSession();
			if(session == null || isBlank(session.getString("externalId"))) {
				userId = UNAUTHENTICATED_USER_ID;
			} else {
				userId = session.getString("externalId");
			}
			tokenId = getTokenHeader(request).orElse(NO_TOKEN_ID);
			final String sessionId = CookieHelper.getInstance().getSigned("oneSessionId", request);
			cookieId = isBlank(sessionId) ? NO_SESSION_COOKIE : sessionId;
			return String.format(" - %s %s %s", userId, cookieId, tokenId);
		} else {
			return "";
		}
	}

	private String getQuery(HttpServerRequest request) {
		if (request.query() != null) {
			return "?" + request.query();
		}
		return "";
	}

}
