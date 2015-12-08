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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.file.FileProps;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.webutils.security.SecuredAction;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

public class StartupUtils {

	private static final Logger log = LoggerFactory.getLogger(StartupUtils.class);

	public static void sendStartup(final JsonObject app, JsonArray actions, final Vertx vertx, Integer appRegistryPort) throws IOException {
		if (actions == null || actions.size() == 0) {
			actions = loadSecuredActions(vertx);
		}
		final String s = new JsonObject().putObject("application", app).putArray("actions", actions).encode();
		final HttpClient httpClient = vertx.createHttpClient().setHost("localhost")
				.setPort(appRegistryPort).setKeepAlive(false);
		httpClient.put("/appregistry/application", new Handler<HttpClientResponse>() {
			@Override
			public void handle(HttpClientResponse event) {
				if (event.statusCode() != 200) {
					log.error("Error recording application : " + s);
					httpClient.close();
				} else {
					final JsonArray widgetsArray = loadWidgets(app.getString("name"), vertx);
					if(widgetsArray.size() == 0){
						httpClient.close();
						return;
					}

					final String widgets = new JsonObject().putArray("widgets", widgetsArray).encode();
					httpClient.post("/appregistry/widget", new Handler<HttpClientResponse>() {
						@Override
						public void handle(HttpClientResponse event) {
							if (event.statusCode() != 200) {
								log.error("Error recording widgets for application " + app.getString("name"));
							} else {
								log.info("Successfully registered widgets for application " + app.getString("name"));
							}
							httpClient.close();
						}
					})
					.putHeader("Content-Type", "application/json")
					.end(widgets);
				}
			}
		})
		.putHeader("Content-Type", "application/json")
		.end(s);
	}

	public static void sendStartup(final JsonObject app, JsonArray actions, final EventBus eb, final String address, final Vertx vertx,
			final Handler<Message<JsonObject>> handler) throws IOException {
		if (actions == null || actions.size() == 0) {
			actions = loadSecuredActions(vertx);
		}
		JsonObject jo = new JsonObject();
		jo.putObject("application", app)
		.putArray("actions", actions);
		eb.send(address, jo, new Handler<Message<JsonObject>>() {
			public void handle(final Message<JsonObject> appEvent) {
				if("error".equals(appEvent.body().getString("status"))){
					log.error("Error registering application " + app.getString("name"));
					if(handler != null) handler.handle(appEvent);
					return;
				}

				final JsonArray widgetsArray = loadWidgets(app.getString("name"), vertx);
				if(widgetsArray.size() == 0){
					if(handler != null) handler.handle(appEvent);
					return;
				}

				final String widgets = new JsonObject().putArray("widgets", widgetsArray).encode();
				eb.send(address+".widgets", widgets, new Handler<Message<JsonObject>>() {
					public void handle(Message<JsonObject> event) {
						if("error".equals(event.body().getString("status"))){
							log.error("Error registering wigets for application " + app.getString("name"));
						} else {
							log.info("Successfully registered widgets for application " + app.getString("name"));
						}
						if(handler != null) handler.handle(appEvent);
					}
				});
			}
		});
	}

	public static void sendStartup(JsonObject app, JsonArray actions, EventBus eb, String address, Vertx vertx) throws IOException {
		sendStartup(app, actions, eb, address, vertx, null);
	}

	public static void sendStartup(JsonObject app, EventBus eb, String address, Vertx vertx,
			final Handler<Message<JsonObject>> handler) throws IOException {
		sendStartup(app, null, eb, address, vertx, handler);
	}

	public static void sendStartup(JsonObject app, EventBus eb, String address) throws IOException {
		sendStartup(app, null, eb, address, null);
	}

	public static JsonArray loadSecuredActions(Vertx vertx) throws IOException {
		String [] list = vertx.fileSystem().readDirSync(".", "^SecuredAction-.*json$");
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
		if (securedActions == null || securedActions.size() == 0) {
			return Collections.emptyMap();
		}
		Map<String, SecuredAction> actions = new HashMap<>();
		for (Object a: securedActions) {
			JsonObject action = (JsonObject) a;
			String name = action.getString("name");
			String displayName = action.getString("displayName");
			String type = action.getString("type");
			if (name != null && type != null && displayName != null
					&& !name.trim().isEmpty() && !type.trim().isEmpty()) {
				actions.put(name, new SecuredAction(name, displayName, type));
			}
		}
		return actions;
	}

	public static JsonArray loadWidgets(String appName, Vertx vertx){
		JsonArray widgets = new JsonArray();

		if(vertx.fileSystem().existsSync("public/widgets")){
			String[] paths = vertx.fileSystem().readDirSync("public/widgets");
			for(final String path: paths){
				FileProps props = vertx.fileSystem().propsSync(path);
				if(props.isDirectory()){
					final String widgetName = new File(path).getName();
					JsonObject widget = new JsonObject()
						.putString("name", new File(widgetName).getName())
						.putString("js", "/public/widgets/"+widgetName+"/"+widgetName+".js")
						.putString("path", "/public/widgets/"+widgetName+"/"+widgetName+".html")
						.putString("applicationName", appName);

					if(vertx.fileSystem().existsSync("public/widgets/"+widgetName+"/i18n")){
						widget.putString("i18n", "/public/widgets/"+widgetName+"/i18n");
					}

					widgets.add(widget);
				}
			}
		}
		return widgets;
	}

}
