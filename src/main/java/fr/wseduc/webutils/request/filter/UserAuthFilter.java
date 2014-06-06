package fr.wseduc.webutils.request.filter;

import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.CookieHelper;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import fr.wseduc.webutils.security.oauth.OAuthResourceProvider;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

public class UserAuthFilter implements Filter {

	private final OAuthResourceProvider oauth;

	public UserAuthFilter() {
		this.oauth = null;
	}

	public UserAuthFilter(OAuthResourceProvider oauth) {
		this.oauth = oauth;
	}

	@Override
	public void canAccess(HttpServerRequest request, Handler<Boolean> handler) {
		String oneSeesionId = CookieHelper.getInstance().getSigned("oneSessionId", request);
		if (oneSeesionId != null && !oneSeesionId.trim().isEmpty()) {
			handler.handle(true);
		} else if (oauth != null && request instanceof SecureHttpServerRequest) {
			oauth.validToken((SecureHttpServerRequest) request, handler);
		} else {
			handler.handle(false);
		}
	}

	@Override
	public void deny(HttpServerRequest request) {
		String callBack = "";
		String location = "";
		String scheme = Renders.getScheme(request);
		String host = Renders.getHost(request);
		try {
			callBack = scheme + "://" + host + request.uri();
			location = scheme + "://" + host;
			if (request.headers().get("X-Forwarded-For") == null) {
				location = location.split(":")[0] + ":8009";
			}
			callBack = URLEncoder.encode(callBack, "UTF-8");
			location += "/auth/login?callback=" + callBack;
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace();
		}
		request.response().setStatusCode(302);
		request.response().putHeader("Location", location);
		request.response().end();
	}

}
