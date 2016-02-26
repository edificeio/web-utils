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

package fr.wseduc.webutils.eventbus;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class ResultMessage implements Message<JsonObject> {

	private final JsonObject body = new JsonObject().putString("status", "ok");

	public ResultMessage put(String attr, Object o) {
		body.putValue(attr, o);
		return this;
	}

	public ResultMessage error(String message) {
		body.putString("status", "error");
		body.putString("message", message);
		return this;
	}

	@Override
	public String address() {
		return null;
	}

	@Override
	public JsonObject body() {
		return body;
	}

	@Override
	public String replyAddress() {
		return null;
	}

	@Override
	public void reply() {

	}

	@Override
	public void reply(Object message) {

	}

	@Override
	public void reply(JsonObject message) {

	}

	@Override
	public void reply(JsonArray message) {

	}

	@Override
	public void reply(String message) {

	}

	@Override
	public void reply(Buffer message) {

	}

	@Override
	public void reply(byte[] message) {

	}

	@Override
	public void reply(Integer message) {

	}

	@Override
	public void reply(Long message) {

	}

	@Override
	public void reply(Short message) {

	}

	@Override
	public void reply(Character message) {

	}

	@Override
	public void reply(Boolean message) {

	}

	@Override
	public void reply(Float message) {

	}

	@Override
	public void reply(Double message) {

	}

	@Override
	public <T1> void reply(Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(Object message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(Object message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(JsonObject message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(JsonObject message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(JsonArray message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(JsonArray message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(String message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(String message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(Buffer message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(Buffer message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(byte[] message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(byte[] message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(Integer message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(Integer message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(Long message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(Long message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(Short message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(Short message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(Character message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(Character message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(Boolean message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(Boolean message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(Float message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(Float message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public <T1> void reply(Double message, Handler<Message<T1>> replyHandler) {

	}

	@Override
	public <T> void replyWithTimeout(Double message, long timeout, Handler<AsyncResult<Message<T>>> replyHandler) {

	}

	@Override
	public void fail(int failureCode, String message) {

	}

}
