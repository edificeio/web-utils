package fr.wseduc.webutils.security.oauth;

import org.vertx.java.core.Handler;

import fr.wseduc.webutils.security.SecureHttpServerRequest;

public interface OAuthResourceProvider {

	void validToken(SecureHttpServerRequest request, Handler<Boolean> handler);

}
