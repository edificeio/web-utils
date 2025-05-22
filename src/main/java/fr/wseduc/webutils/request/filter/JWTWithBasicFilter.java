package fr.wseduc.webutils.request.filter;

import fr.wseduc.webutils.collections.SharedDataHelper;
import fr.wseduc.webutils.security.JWT;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class JWTWithBasicFilter {

	private static final Logger log = LoggerFactory.getLogger(JWTWithBasicFilter.class);

	private final AbstractBasicFilter basicFilter;
	private JWT jwt;

	public JWTWithBasicFilter(AbstractBasicFilter basicFilter) {
		this.basicFilter = basicFilter;
	}

	public void init(Vertx vertx) {
		SharedDataHelper.getInstance().<String, String>get("server", "signKey")
				.onSuccess(signKey -> jwt = new JWT(vertx, signKey, null))
				.onFailure(ex -> log.error("Error getting jwt signKey", ex));
	}

	public void validate(final SecureHttpServerRequest request, final Handler<Boolean> handler) {
		if (jwt == null) {
			handler.handle(false);
			return;
		}
		basicFilter.validate(request, basicResult -> {
			if (basicResult) {
				final String[] authorizations = request.headers().get("Authorization").split(",\\s*");
				if (authorizations.length == 2 && authorizations[1].startsWith("Bearer ")) {
					request.pause();
					jwt.verifyAndGet(authorizations[1].substring(7), payload -> {
						request.resume();
						if (payload != null &&
								payload.getLong("exp", 0L) > (System.currentTimeMillis() / 1000) &&
								payload.getString("aud") != null &&
								payload.getString("aud").equals(request.getAttribute("client_id"))) {
							request.setAttribute("remote_user", payload.getString("sub"));
							request.setAttribute("authorization_type", "Bearer");
							handler.handle(true);
						} else {
							handler.handle(false);
						}
					});
				} else {
					handler.handle(false);
				}
			} else {
				handler.handle(false);
			}
		});
	}

	public boolean hasBasicAndJWTHeader(HttpServerRequest request) {
		final String authorization = request.headers().get("Authorization");
		return authorization != null &&
				authorization.startsWith("Basic ") &&
				authorization.contains("Bearer ");
	}

}
