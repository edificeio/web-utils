package fr.wseduc.webutils.http;

import fr.wseduc.webutils.Controller;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.security.SecuredAction;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Container;

import java.util.Map;

public class BaseController extends Controller {

	private BaseController(Vertx vertx, Container container, RouteMatcher rm,
						   Map<String, SecuredAction> securedActions) {
		super(vertx, container, rm, securedActions);
	}

	public BaseController() {
		this(null, null, null, null);
	}

	public void init(Vertx vertx, Container container, RouteMatcher rm,
					 Map<String, SecuredAction> securedActions) {
		super.vertx = vertx;
		super.container = container;
		super.rm = rm;
		super.securedActions = securedActions;
		super.eb = Server.getEventBus(vertx);
		if (pathPrefix == null) {
			super.pathPrefix = Server.getPathPrefix(container.config());
		}
		if (rm != null) {
			loadRoutes();
		} else {
			log.error("RouteMatcher is null.");
		}
	}

}
