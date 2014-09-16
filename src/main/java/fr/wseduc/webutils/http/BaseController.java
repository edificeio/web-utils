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
