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

package fr.wseduc.webutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import io.micrometer.common.util.StringUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileProps;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static fr.wseduc.webutils.data.FileResolver.absolutePath;

public class StartupUtils {

	private static final Logger log = LoggerFactory.getLogger(StartupUtils.class);

	public static void sendStartup(final JsonObject app, JsonArray actions, final Vertx vertx, Integer appRegistryPort) throws IOException {
		if (actions == null || actions.size() == 0) {
			actions = loadSecuredActions(vertx);
		}
		final String s = new JsonObject().put("application", app).put("actions", actions).encode();
		final HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions()
				.setDefaultHost("localhost").setDefaultPort(appRegistryPort).setKeepAlive(false));
		httpClient.request(HttpMethod.PUT, "/appregistry/application")
		.map(r -> r.putHeader("Content-Type", "application/json"))
		.flatMap(req -> req.send(s))
		.onSuccess(event -> {
			if (event.statusCode() != 200) {
				log.error("Error recording application : " + s);
				httpClient.close();
			} else {
				final JsonArray widgetsArray = loadWidgets(app.getString("name"), vertx);
				if(widgetsArray.isEmpty()){
					httpClient.close();
					return;
				}

				final String widgets = new JsonObject().put("widgets", widgetsArray).encode();
				httpClient.request(HttpMethod.POST, "/appregistry/widget")
				.map(r -> r.putHeader("Content-Type", "application/json"))
				.flatMap(req -> req.send(widgets))
				.onSuccess(res -> {
						if (res.statusCode() != 200) {
							log.error("Error recording widgets for application " + app.getString("name"));
						} else {
							log.info("Successfully registered widgets for application " + app.getString("name"));
						}
						httpClient.close();
				})
				.onFailure(exceptw -> log.error("Error sending widgets to appregistry : " + widgets, exceptw));
			}
		})
		.onFailure(except -> log.error("Error sending application to appregistry : " + s, except));
	}

	public static void sendStartup(final JsonObject app, JsonArray actions, final EventBus eb, final String address, final Vertx vertx,
			final Handler<AsyncResult<Message<JsonObject>>> handler) throws IOException {
		if (actions == null || actions.size() == 0) {
			actions = loadSecuredActions(vertx);
		}
		JsonObject jo = new JsonObject();
		jo.put("application", app)
		.put("actions", applyOverrideRightForRegistry(actions));
		eb.request(address, jo, (AsyncResult<Message<JsonObject>> appEvent) -> {
				if(appEvent.failed()){
					log.error("Error registering application " + app.getString("name"), appEvent.cause());
					if(handler != null) handler.handle(appEvent);
					return;
				}

				final JsonArray widgetsArray = loadWidgets(app.getString("name"), vertx);
				if(widgetsArray.size() == 0){
					if(handler != null) handler.handle(appEvent);
					return;
				}

				final JsonObject widgets = new JsonObject().put("widgets", widgetsArray);
				eb.request(address+".widgets", widgets, (Handler<AsyncResult<Message<JsonObject>>>) event -> {
          if(event.failed()){
            log.error("Error registering widgets for application " + app.getString("name"), event.cause());
          } else {
            log.info("Successfully registered widgets for application " + app.getString("name"));
          }
          if(handler != null) handler.handle(appEvent);
        });
		});
	}

	public static void sendStartup(JsonObject app, JsonArray actions, EventBus eb, String address, Vertx vertx) throws IOException {
		sendStartup(app, actions, eb, address, vertx, null);
	}

	public static void sendStartup(JsonObject app, EventBus eb, String address, Vertx vertx,
			final Handler<AsyncResult<Message<JsonObject>>> handler) throws IOException {
		sendStartup(app, null, eb, address, vertx, handler);
	}

	public static void sendStartup(JsonObject app, EventBus eb, String address) throws IOException {
		sendStartup(app, null, eb, address, null);
	}

	public static JsonArray loadSecuredActions(Vertx vertx) throws IOException {
		List<String> list = vertx.fileSystem().readDirBlocking(absolutePath("securedaction"), "^SecuredAction-.*json$");
		JsonArray securedActions = new JsonArray();
		for (String f : list) {
			BufferedReader in = null;
			try {
				in = new BufferedReader(new FileReader(f));
				String line;
				while((line = in.readLine()) != null) {
					securedActions.add(new JsonObject(line));
				}
			} finally {
				if (in != null) {
					in.close();
				}
			}
		}
		return securedActions;
	}

	public static Map<String, SecuredAction> securedActionsToMap(JsonArray securedActions) {
		if (securedActions == null || securedActions.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, SecuredAction> actions = new HashMap<>();
		for (Object a: securedActions) {
			JsonObject action = (JsonObject) a;
			String qualifiedName = action.getString("name");
			String displayName = action.getString("displayName");
			String type = action.getString("type");
			String right = action.getString("right");
			if (qualifiedName != null && type != null && displayName != null
					&& !qualifiedName.trim().isEmpty() && !type.trim().isEmpty()) {
				actions.put(qualifiedName, new SecuredAction(qualifiedName, displayName, type, right));
			}
		}
		return actions;
	}

	public static Map<String, SecuredAction> applyOverrideRightForShare(Map<String, SecuredAction> securedActionMap) {
		Map<String, SecuredAction> toReturn = new HashMap<>();
		if (securedActionMap == null) {
			return toReturn;
		}
        for (Map.Entry<String, SecuredAction> entry : securedActionMap.entrySet()) {
            //we want to use right as replacement for name and this key is unique
			toReturn.put(entry.getValue().getRight(), entry.getValue());

			//duplicate between override right and legacy right
			// override on a right on different type should not be allowed to avoid weird behaviour
            SecuredAction action = securedActionMap.get(entry.getValue().getRight());
            if (action != null && !action.getType().equals(entry.getValue().getType())) {
                throw new IllegalArgumentException(String.format(" %s override right has a type " +
                        "different from overridden action ", entry.getValue().getName()));
            }
        }
		return toReturn;
	}

	public static JsonArray applyOverrideRightForRegistry(JsonArray securedActions) {
		//replace name ( class name + | + method) by the right
		ArrayList<JsonObject> toReturn = new ArrayList<>();
		for (Object oAction : securedActions.getList()) {
			JsonObject action = ((JsonObject) oAction).copy();
			action.put("name", action.getString("right"));
			toReturn.add(action);
		}
		// unicity on name + consistency control
		for (Iterator<JsonObject> it = toReturn.iterator(); it.hasNext();) {
			JsonObject action = it.next();
			List<JsonObject> duplicateAction = toReturn.stream()
					.filter(a -> a.getString("name")
					.equals(action.getString("name")))
					.collect(Collectors.toList());
			if(duplicateAction.size() > 1) {
				//verify type coherence
				if (duplicateAction.stream().anyMatch(a -> !a.getString("type").equals(action.getString("type")))) {
					throw new IllegalArgumentException((String.format(" %s override right has a type " +
							"different from overridden action ", action.getString("name") )));
				}
				it.remove();
			}
		}
		return new JsonArray(toReturn);
	}

	public static JsonArray loadWidgets(String appName, Vertx vertx){
		JsonArray widgets = new JsonArray();

		if(vertx.fileSystem().existsBlocking("public/widgets")){
			List<String> paths = vertx.fileSystem().readDirBlocking("public/widgets");
			for(final String path: paths){
				FileProps props = vertx.fileSystem().propsBlocking(path);
				if(props.isDirectory()){
					final String widgetName = new File(path).getName();
					JsonObject widget = new JsonObject()
						.put("name", new File(widgetName).getName())
						.put("js", "/public/widgets/"+widgetName+"/"+widgetName+".js")
						.put("path", "/public/widgets/"+widgetName+"/"+widgetName+".html")
						.put("applicationName", appName);

					if(vertx.fileSystem().existsBlocking("public/widgets/" + widgetName + "/i18n")){
						widget.put("i18n", "/public/widgets/"+widgetName+"/i18n");
					}

					widgets.add(widget);
				}
			}
		}
		return widgets;
	}

}
