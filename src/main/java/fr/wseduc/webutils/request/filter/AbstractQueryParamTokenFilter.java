package fr.wseduc.webutils.request.filter;


import fr.wseduc.webutils.collections.SharedDataHelper;
import fr.wseduc.webutils.security.JWT;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

abstract public class AbstractQueryParamTokenFilter {

	private static final Logger log = LoggerFactory.getLogger(AbstractQueryParamTokenFilter.class);
	public static String QUERYPARAM_TOKEN = "queryparam_token";
	private JWT jwt;

	public AbstractQueryParamTokenFilter init(Vertx vertx) {
		SharedDataHelper.getInstance().<String, String>getLocal("server", "signKey")
				.onSuccess(signKey -> jwt = new JWT(vertx, signKey, null))
				.onFailure(ex -> log.error("Error getting jwt signKey", ex));
		return this;
	}

	public String getToken(final SecureHttpServerRequest request) {
		return request.getParam(QUERYPARAM_TOKEN);
	}

	public void validate(final SecureHttpServerRequest request, final Handler<Boolean> handler) {
		final String token = getToken(request);
		if (jwt == null || token == null || token.length() == 0) {
			handler.handle(false);
			return;
		}

		request.pause();
		jwt.verifyAndGet(token, payload -> {
			request.resume();
			if (payload == null 
					|| payload.getLong("exp", 0L) < (System.currentTimeMillis() / 1000)
					|| payload.getString("aud") == null || payload.getString("aud").trim().isEmpty()) {
				// Invalid token
				handler.handle(false);
				return;
			}
			final String clientId = payload.getString("aud");
			// Token has been verified, let's retrieve its associated scope.
			request.pause();
			retrieveClientScope(clientId, scope -> {
				request.resume();
				boolean res = scope != null && !scope.trim().isEmpty();
				if (res) {
					request.setAttribute("client_id", clientId);
					request.setAttribute("remote_user", payload.getString("sub"));
					request.setAttribute("authorization_type", "Bearer");
					request.setAttribute("scope", scope);

					handler.handle(customValidation(request));
				} else {
					handler.handle(false);
				}
			});
		});
	}

	protected abstract void retrieveClientScope(String clientId, Handler<String> handler);

	protected boolean customValidation(SecureHttpServerRequest request) {
		return true;
	}

}
