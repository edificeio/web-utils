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
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

public interface SendEmail {

	String getSenderEmail();

	String getHost(HttpServerRequest request);

	void sendEmail(HttpServerRequest request, String to, String cc, String bcc,
						  String subject, String templateBody, JsonObject templateParams,
						  boolean translateSubject, final Handler<Message<JsonObject>> handler);

	void sendEmail(HttpServerRequest request, String to, String cc, String bcc,
				   String subject, JsonArray attachments, String templateBody, JsonObject templateParams,
				   boolean translateSubject, final Handler<Message<JsonObject>> handler);

	void sendEmail(HttpServerRequest request, String to, String cc, String bcc,
						  String subject, String templateBody, JsonObject templateParams,
						  boolean translateSubject, JsonArray headers, final Handler<Message<JsonObject>> handler);

	void sendEmail(HttpServerRequest request, String to, String from, String cc, String bcc,
						  String subject, String templateBody, JsonObject templateParams,
						  boolean translateSubject, final Handler<Message<JsonObject>> handler);

	void sendEmail(HttpServerRequest request, String to, String from, String cc, String bcc,
				   String subject, String templateBody, JsonObject templateParams,
				   boolean translateSubject, JsonArray headers, final Handler<Message<JsonObject>> handler);

	void sendEmail(HttpServerRequest request, String to, String from, String cc, String bcc,
				   String subject, JsonArray attachments, String templateBody, JsonObject templateParams,
				   boolean translateSubject, JsonArray headers, final Handler<Message<JsonObject>> handler);

	void sendEmail(HttpServerRequest request, List<Object> to, List<Object> cc, List<Object> bcc,
						  String subject, String templateBody, JsonObject templateParams,
						  boolean translateSubject, final Handler<Message<JsonObject>> handler);

	void sendEmail(HttpServerRequest request, List<Object> to, String from, List<Object> cc, List<Object> bcc,
				   String subject, String templateBody, JsonObject templateParams,
				   boolean translateSubject, JsonArray headers, final Handler<Message<JsonObject>> handler);

	void sendEmail(HttpServerRequest request, List<Object> to, String from, List<Object> cc, List<Object> bcc,
				   String subject, JsonArray attachments, String templateBody, JsonObject templateParams,
				   boolean translateSubject, JsonArray headers, final Handler<Message<JsonObject>> handler);

}
