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

package fr.wseduc.webutils.email;


import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.io.UnsupportedEncodingException;

public class BusMailSender extends NotificationHelper {

	protected String emailAddress;
	private final EventBus eb;

	protected BusMailSender(Vertx vertx, Container container) {
		this(vertx, container, null);
	}

	public BusMailSender(Vertx vertx, Container container, String emailAddress) {
		super(vertx, container);
		this.eb = vertx.eventBus();
		this.emailAddress = emailAddress;
	}

	protected void sendEmail(JsonObject json, Handler<Message<JsonObject>> handler) {
		try {
			json.putString("body", new String(json.getString("body").getBytes("UTF-8"), "ISO-8859-1"));
			eb.send(emailAddress, json, handler);
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage(), e);
			Message<JsonObject> m = new ErrorMessage();
			m.body().putString("error", e.getMessage());
			handler.handle(m);
		}
	}

}
