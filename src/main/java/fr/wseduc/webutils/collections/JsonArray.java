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

package fr.wseduc.webutils.collections;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class JsonArray extends io.vertx.core.json.JsonArray {

	private final List<Object> list;

	public JsonArray(String json) {
		this(Json.decodeValue(json, List.class));
	}

	public JsonArray() {
		this(new ArrayList<>());
	}

	public JsonArray(List list) {
		super(list);
		this.list = list;
	}

	public JsonArray(Buffer buf) {
		this(Json.decodeValue(buf, List.class));
	}

	@Override
	public io.vertx.core.json.JsonArray add(Enum value) {
		list.add(value);
		return this;
	}

	@Override
	public io.vertx.core.json.JsonArray add(CharSequence value) {
		list.add(value);
		return this;
	}

	@Override
	public io.vertx.core.json.JsonArray add(String value) {
		list.add(value);
		return this;
	}

	@Override
	public io.vertx.core.json.JsonArray add(Integer value) {
		list.add(value);
		return this;
	}

	@Override
	public io.vertx.core.json.JsonArray add(Long value) {
		list.add(value);
		return this;
	}

	@Override
	public io.vertx.core.json.JsonArray add(Double value) {
		list.add(value);
		return this;
	}

	@Override
	public io.vertx.core.json.JsonArray add(Float value) {
		list.add(value);
		return this;
	}

	@Override
	public io.vertx.core.json.JsonArray add(Boolean value) {
		list.add(value);
		return this;
	}

	@Override
	public io.vertx.core.json.JsonArray add(JsonObject value) {
		list.add(value);
		return this;
	}

	@Override
	public io.vertx.core.json.JsonArray add(io.vertx.core.json.JsonArray value) {
		list.add(value);
		return this;
	}

	@Override
	public io.vertx.core.json.JsonArray add(byte[] value) {
		list.add(value);
		return this;
	}

	@Override
	public io.vertx.core.json.JsonArray add(Instant value) {
		list.add(value);
		return this;
	}

	@Override
	public io.vertx.core.json.JsonArray add(Object value) {
		list.add(value);
		return this;
	}

	public static io.vertx.core.json.JsonArray setInJsonArray(io.vertx.core.json.JsonArray a, int pos, String value)
	{
		// Vertx doesn't support set in 3.5.0 JsonArrays...
		if(pos < 0 || pos > a.size())
			throw new ArrayIndexOutOfBoundsException();
		else if(pos == a.size())
			a.add(value);
		else
		{
			List aList = a.getList();
			aList.set(pos, value);
		}
		return a;
	}

	public static io.vertx.core.json.JsonArray setInJsonArray(io.vertx.core.json.JsonArray a, int pos, Object value)
	{
		// Vertx doesn't support set in 3.5.0 JsonArrays...
		if(pos < 0 || pos > a.size())
			throw new ArrayIndexOutOfBoundsException();
		else if(pos == a.size())
			a.add(value);
		else
		{
			List aList = a.getList();
			aList.set(pos, value);
		}
		return a;
	}

}
