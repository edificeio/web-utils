/*
 * Copyright © "Open Digital Education", 2016
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 */

package fr.wseduc.sms;

import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.StringValidation;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import org.apache.commons.lang3.StringUtils;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

//-------------------
public class Sms {

	private static final Logger log = LoggerFactory.getLogger(Sms.class);
//-------------------

	//--------------------------------------
	private static class SmsFactoryHolder {
	//--------------------------------------
		private static final SmsFactory instance = new SmsFactory();
	}

	//--------------------------------------
	public static class SmsFactory {
	//--------------------------------------
		private Vertx vertx;
		private EventBus eb;
		private JsonObject config;
		private String smsProvider;
		private String smsAddress;

		protected SmsFactory() {
		}

		public void init(Vertx vertx, JsonObject config) {
			this.eb = Server.getEventBus(vertx);
			this.vertx = vertx;
			this.config = config;
			LocalMap<Object, Object> server = vertx.sharedData().getLocalMap("server");
			if(server != null && server.get("smsProvider") != null) {
				smsProvider = (String) server.get("smsProvider");
				final String node = (String) server.get("node");
				smsAddress = (node != null ? node : "") + "entcore.sms";
			} else {
				smsAddress = "entcore.sms";
			}
		}

		public Sms newInstance( Renders render ) {
			return new Sms( (render == null) ? new Renders(vertx, config) : render );
		}


		protected JsonObject getSmsObjectFor(final String target, final String body) {
			return new JsonObject()
			.put("provider", smsProvider)
			.put("action", "send-sms")
			.put("parameters", new JsonObject()
				.put("receivers", new fr.wseduc.webutils.collections.JsonArray().add(target))
				.put("message", body)
				.put("senderForResponse", true)
				.put("noStopClause", true));
		}

		protected Future<SmsSendingReport> send(final JsonObject smsObject) {
			Promise<SmsSendingReport> promise = Promise.promise();
			eb.request(smsAddress, smsObject, handlerToAsyncHandler(event -> {
				if("error".equals(event.body().getString("status"))){
					promise.fail(event.body().getString("message", ""));
				} else {
					try {
						final SmsSendingReport report = event.body().getJsonObject("data").mapTo(SmsSendingReport.class);
						promise.complete(report);
					} catch(Exception e) {
						log.error("An error occurred while deserializing data from sms sender to class SmsSendingReport. Data to deserialize were " + event.body(), e);
						promise.fail(e);
					}
				}
			}));
			return promise.future();
		}
	}

	////////////////////////////////////////

	public static SmsFactory getFactory() {
		return SmsFactoryHolder.instance;
	}

	////////////////////////////////////////

	private Renders render;

	private Sms(final Renders render) {
		this.render = render;
	}

	public Future<SmsSendingReport> send(HttpServerRequest request, final String phone, String template, JsonObject params){
		return send(request, phone, template, params, null);
	}

	/**
	 * @param request Http request that triggered the sms
	 * @param phone Complete phone number of the user
	 * @param template Template to use to generate the body of the template
	 * @param params Parameters to populate the SMS template
	 * @param module (optional) Name of the module that requested the SMS
	 * @return A report of the send job
	 */
	public Future<SmsSendingReport> send(HttpServerRequest request, final String phone, String template, JsonObject params, final String module){
		return processTemplate(request, template, params)
				.compose(body -> send(phone, body));
	}

	/**
	 * Send a message to a phone number
	 * @param phone complete phone number of the user
	 * @param message the message to be sent
	 * @return a report of the send job
	 */
	public Future<SmsSendingReport> send(String phone, String message) {
		if (StringUtils.isBlank(phone)) {
			return Future.failedFuture("empty.target");
		} else {
			final String formattedPhone = StringValidation.formatPhone(phone);
			final JsonObject smsObject = getFactory().getSmsObjectFor(formattedPhone, message);
			return getFactory().send(smsObject);
		}
	}

	private Future<String> processTemplate(HttpServerRequest request, String template, JsonObject params){
		Promise<String> promise = Promise.promise();
		render.processTemplate(request, template, params, body -> {
			if (body != null) {
				promise.complete(body);
			} else {
				promise.fail("template.error");
			}
		});
		return promise.future();
	}
}